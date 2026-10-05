package com.crownofchaos.unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.faction.Faction;
import com.crownofchaos.faction.FactionData;
import com.crownofchaos.item.ModItems;
import com.crownofchaos.network.UnitCommandPayload;
import com.crownofchaos.network.UnitSelectionPayload;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Commanding units. A player can command their faction's units whose crown they're wearing.
 * <ul>
 * <li>Right-click a unit: it holds its position (sentry), or is freed again.
 * <li>Sneak and right-click a unit: add it to the selection, or remove it. Selected units glow
 *     for that player.
 * <li>The command menu and the follow key send an order (see {@link UnitCommandPayload}) to the
 *     selected units, or, with none selected, to every unit within {@link #COMMAND_RANGE} the
 *     player can command.
 * </ul>
 * A unit's owner is told when it dies out of their sight (see {@link #canSee}).
 */
public final class UnitCommands {
	/** With nothing selected, orders go to every commandable unit this close. */
	public static final double COMMAND_RANGE = 32.0;
	private static final ChatFormatting INFO = ChatFormatting.LIGHT_PURPLE;
	private static final ChatFormatting ERROR = ChatFormatting.RED;

	/** Each player's selected units. Only touched on the server thread. */
	private static final Map<UUID, Set<UUID>> SELECTION = new HashMap<>();

	private UnitCommands() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(UnitCommandPayload.TYPE, UnitCommandPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(UnitSelectionPayload.TYPE, UnitSelectionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UnitCommandPayload.TYPE, (payload, context) -> command(context.player(), payload.command()));

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)
					|| !(entity instanceof Mob mob)) {
				return InteractionResult.PASS;
			}
			return clickUnit(serverPlayer, mob);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			UnitData data = entity.getAttached(UnitManager.UNIT);
			if (data != null && entity.level().getServer() != null) {
				tellOwnerOfDeath(entity.level().getServer(), entity, data);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				pruneSelections(server);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SELECTION.remove(handler.getPlayer().getUUID()));
	}

	// Who can command what

	/** Whether the player can command the unit: it's their faction's, and they wear its crown. */
	public static boolean canCommand(Player player, Entity unit) {
		UnitData data = unit.getAttached(UnitManager.UNIT);
		if (data == null || player.level().getServer() == null) {
			return false;
		}
		Optional<Faction> faction = FactionData.get(player.level().getServer()).factionOf(player.getUUID());
		return faction.isPresent() && faction.get().id().equals(data.faction()) && wearsCrownFor(player, data);
	}

	/** Whether the player wears the crown (or its netherite version) that commands the unit. */
	public static boolean wearsCrownFor(Player player, UnitData data) {
		UnitType type = ModUnits.byId(data.unit());
		return type != null && ModItems.baseCrown(player.getItemBySlot(EquipmentSlot.HEAD).getItem()) == type.crown();
	}

	// Clicking units

	private static InteractionResult clickUnit(ServerPlayer player, Mob mob) {
		UnitData data = mob.getAttached(UnitManager.UNIT);
		if (data == null) {
			return InteractionResult.PASS;
		}
		Optional<Faction> faction = FactionData.get(player.level().getServer()).factionOf(player.getUUID());
		if (faction.isEmpty() || !faction.get().id().equals(data.faction())) {
			return InteractionResult.PASS;
		}
		if (!wearsCrownFor(player, data)) {
			UnitType type = ModUnits.byId(data.unit());
			if (type != null) {
				player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.wrong_crown",
						Component.translatable(type.crown().getDescriptionId())).withStyle(ERROR));
			}
			return InteractionResult.PASS;
		}

		if (player.isShiftKeyDown()) {
			Set<UUID> selected = SELECTION.computeIfAbsent(player.getUUID(), id -> new LinkedHashSet<>());
			boolean added = selected.add(mob.getUUID());
			if (!added) {
				selected.remove(mob.getUUID());
			}
			player.sendOverlayMessage(Component.translatable(added ? "message.crownofchaos.unit.selected" : "message.crownofchaos.unit.deselected",
					mob.getName(), selected.size()).withStyle(INFO));
			sendSelection(player);
		} else if (data.order() == UnitOrder.SENTRY) {
			order(mob, UnitOrder.FREE, player);
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.sentry_off", mob.getName()).withStyle(INFO));
		} else {
			order(mob, UnitOrder.SENTRY, player);
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.sentry_on", mob.getName()).withStyle(INFO));
		}
		return InteractionResult.SUCCESS;
	}

	// Orders

	private static void command(ServerPlayer player, UnitCommandPayload.Command command) {
		switch (command) {
			case CLEAR_SELECTION -> {
				SELECTION.remove(player.getUUID());
				sendSelection(player);
				player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.selection_cleared").withStyle(INFO));
				return;
			}
			case SELECT_NEARBY -> {
				List<Mob> nearby = nearbyUnits(player);
				Set<UUID> selected = SELECTION.computeIfAbsent(player.getUUID(), id -> new LinkedHashSet<>());
				nearby.forEach(mob -> selected.add(mob.getUUID()));
				sendSelection(player);
				player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.selected_nearby", nearby.size(), selected.size()).withStyle(INFO));
				return;
			}
			default -> {
			}
		}

		List<Mob> units = commandedUnits(player);
		if (units.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.no_units").withStyle(ERROR));
			return;
		}
		UnitOrder order = switch (command) {
			case FOLLOW -> UnitOrder.FOLLOW;
			case SENTRY -> UnitOrder.SENTRY;
			case ATTACK -> UnitOrder.ATTACK;
			case FREE -> UnitOrder.FREE;
			case TOGGLE_FOLLOW -> units.stream().allMatch(mob -> isFollowing(mob, player)) ? UnitOrder.FREE : UnitOrder.FOLLOW;
			default -> null;
		};
		if (order == null) {
			reclaim(player, units);
			return;
		}
		units.forEach(mob -> order(mob, order, player));
		player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.ordered." + order.getSerializedName(), units.size()).withStyle(INFO));
	}

	/** Gives the unit an order: a sentry guards the spot it's on, a follower follows the player. */
	private static void order(Mob mob, UnitOrder order, ServerPlayer player) {
		UnitData data = mob.getAttached(UnitManager.UNIT);
		if (data == null) {
			return;
		}
		mob.setAttached(UnitManager.UNIT, data.withOrder(order,
				order == UnitOrder.SENTRY ? Optional.of(mob.blockPosition()) : Optional.empty(),
				order == UnitOrder.FOLLOW ? Optional.of(player.getUUID()) : Optional.empty()));
		// A new order replaces whatever it was doing, fights included (attacking units find new targets).
		UnitGoals.clearTarget(mob);
		mob.getNavigation().stop();
	}

	private static boolean isFollowing(Mob mob, ServerPlayer player) {
		UnitData data = mob.getAttached(UnitManager.UNIT);
		return data != null && data.order() == UnitOrder.FOLLOW && data.leader().filter(player.getUUID()::equals).isPresent();
	}

	/**
	 * Turns the units back into their items, into the player's inventory (or dropped if it's
	 * full). Not while their faction is at war.
	 */
	private static void reclaim(ServerPlayer player, List<Mob> units) {
		Optional<Faction> faction = FactionData.get(player.level().getServer()).factionOf(player.getUUID());
		if (faction.isPresent() && faction.get().atWar()) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.reclaim_at_war").withStyle(ERROR));
			return;
		}
		int count = 0;
		for (Mob mob : units) {
			UnitData data = mob.getAttached(UnitManager.UNIT);
			UnitType type = data == null ? null : ModUnits.byId(data.unit());
			if (type == null) {
				continue;
			}
			player.getInventory().placeItemBackInInventory(new ItemStack(type.item()));
			mob.discard();
			count++;
		}
		Set<UUID> selected = SELECTION.get(player.getUUID());
		if (selected != null) {
			units.forEach(mob -> selected.remove(mob.getUUID()));
			sendSelection(player);
		}
		player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.reclaimed", count).withStyle(INFO));
	}

	/** The units an order goes to: the player's selection, or with none, every commandable unit nearby. */
	private static List<Mob> commandedUnits(ServerPlayer player) {
		List<Mob> selected = selectedUnits(player);
		return selected.isEmpty() ? nearbyUnits(player) : selected;
	}

	private static List<Mob> nearbyUnits(ServerPlayer player) {
		return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COMMAND_RANGE),
				mob -> mob.isAlive() && canCommand(player, mob) && mob.distanceTo(player) <= COMMAND_RANGE);
	}

	/** The player's selected units that are alive, loaded, nearby enough to see, and still theirs to command. */
	private static List<Mob> selectedUnits(ServerPlayer player) {
		Set<UUID> selected = SELECTION.get(player.getUUID());
		List<Mob> units = new ArrayList<>();
		if (selected == null) {
			return units;
		}
		ServerLevel level = (ServerLevel) player.level();
		for (UUID id : selected) {
			if (level.getEntity(id) instanceof Mob mob && mob.isAlive() && canCommand(player, mob)) {
				units.add(mob);
			}
		}
		return units;
	}

	// Selection glow

	private static void sendSelection(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, UnitSelectionPayload.TYPE)) {
			ServerPlayNetworking.send(player, new UnitSelectionPayload(selectedUnits(player).stream().map(Entity::getId).toList()));
		}
	}

	/** Every second: forgets selected units that died or were removed, and updates the glow. */
	private static void pruneSelections(MinecraftServer server) {
		for (Map.Entry<UUID, Set<UUID>> entry : SELECTION.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				continue;
			}
			ServerLevel level = (ServerLevel) player.level();
			boolean changed = entry.getValue().removeIf(id -> {
				Entity entity = level.getEntity(id);
				return entity != null && !entity.isAlive();
			});
			if (changed) {
				sendSelection(player);
			}
		}
	}

	// Deaths

	/** Units further than this count as out of sight. */
	private static final double SIGHT_DISTANCE = 64.0;
	/** Units more than this many degrees off where the player is looking count as out of sight. */
	private static final double SIGHT_ANGLE = 70.0;

	/** Whether the player can actually see the unit: near enough, roughly in front of them, and not hidden by blocks. */
	private static boolean canSee(ServerPlayer player, Entity unit) {
		if (player.level() != unit.level() || player.distanceTo(unit) > SIGHT_DISTANCE || !player.hasLineOfSight(unit)) {
			return false;
		}
		Vec3 toUnit = unit.getEyePosition().subtract(player.getEyePosition()).normalize();
		double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, player.getLookAngle().dot(toUnit)))));
		return angle <= SIGHT_ANGLE;
	}

	/** Tells the unit's owner it died, unless they saw it happen. */
	private static void tellOwnerOfDeath(MinecraftServer server, Entity unit, UnitData data) {
		@Nullable ServerPlayer owner = server.getPlayerList().getPlayer(data.owner());
		if (owner == null || canSee(owner, unit)) {
			return;
		}
		owner.sendSystemMessage(Component.translatable("message.crownofchaos.unit.died", unit.getName(),
				unit.blockPosition().getX(), unit.blockPosition().getY(), unit.blockPosition().getZ()).withStyle(ChatFormatting.RED));
	}
}

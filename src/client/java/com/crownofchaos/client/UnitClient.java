package com.crownofchaos.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.crownofchaos.client.faction.FactionClientState;
import com.crownofchaos.client.faction.UnitCommandOverlay;
import com.crownofchaos.network.UnitCommandPayload;
import com.crownofchaos.network.UnitSelectionPayload;
import com.crownofchaos.unit.UnitCommands;
import com.crownofchaos.unit.UnitData;
import com.crownofchaos.unit.UnitManager;
import com.mojang.blaze3d.platform.InputConstants;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Client side of unit commands: the "Unit Commands" key (default V) toggles the command overlay (the number keys give its orders), the
 * "Units: Follow / Stop Following" key (default B) toggles follow, selected units glow, and
 * right-clicking one of your units is handed to the server instead of the mob's usual
 * interaction (see {@link UnitCommands}).
 */
public final class UnitClient {
	private static final KeyMapping COMMAND_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.crownofchaos.unit_commands", InputConstants.Type.KEYSYM, InputConstants.KEY_V, FactionKeybind.CATEGORY));
	private static final KeyMapping FOLLOW_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.crownofchaos.unit_follow", InputConstants.Type.KEYSYM, InputConstants.KEY_B, FactionKeybind.CATEGORY));

	/** Whether units always show an outline in their faction's color (only for units in view, see {@link #showsOutline}). */
	private static final boolean OUTLINE_ALL_UNITS = true;
	/** Units farther than this many blocks show no outline. */
	private static final double OUTLINE_RANGE = 64.0;

	/** Per unit entity id: the game tick it was last checked, and whether the player could see it then (1) or not (0). */
	private static final Map<Integer, long[]> LINE_OF_SIGHT = new HashMap<>();

	/** Entity ids of the units this player has selected, as last sent by the server. */
	private static final Set<Integer> SELECTED = new IntOpenHashSet();

	private UnitClient() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(UnitSelectionPayload.TYPE, (payload, context) -> {
			SELECTED.clear();
			SELECTED.addAll(payload.entityIds());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SELECTED.clear();
			LINE_OF_SIGHT.clear();
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("crownofchaos", "unit_commands"), new UnitCommandOverlay());

		// Before vanilla reads the keys this tick, so the number keys give orders instead of switching the hotbar.
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (UnitCommandOverlay.isOpen() && (client.player == null || client.gui.screen() != null)) {
				UnitCommandOverlay.setOpen(false);
			}
			if (!UnitCommandOverlay.isOpen()) {
				return;
			}
			KeyMapping[] hotbar = client.options.keyHotbarSlots;
			for (int i = 0; i < UnitCommandOverlay.orderCount() && i < hotbar.length; i++) {
				boolean pressed = false;
				while (hotbar[i].consumeClick()) {
					pressed = true;
				}
				if (pressed && UnitCommandOverlay.isOpen()) {
					UnitCommandOverlay.give(i);
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (COMMAND_KEY.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					UnitCommandOverlay.setOpen(!UnitCommandOverlay.isOpen());
				}
			}
			while (FOLLOW_KEY.consumeClick()) {
				if (client.player != null && ClientPlayNetworking.canSend(UnitCommandPayload.TYPE)) {
					ClientPlayNetworking.send(new UnitCommandPayload(UnitCommandPayload.Command.TOGGLE_FOLLOW));
				}
			}
		});

		// Right-clicking one of your units goes to the server's unit commands, not the mob's usual interaction.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
				level.isClientSide() && hand == InteractionHand.MAIN_HAND && canCommand(player, entity)
						? InteractionResult.SUCCESS : InteractionResult.PASS);
	}

	/** Whether the selected-unit glow should show on this entity. */
	public static boolean isSelected(Entity entity) {
		return SELECTED.contains(entity.getId());
	}

	/** Whether the entity is a unit in view and close enough to show its faction-colored outline. */
	public static boolean showsOutline(Entity entity) {
		Minecraft client = Minecraft.getInstance();
		if (!OUTLINE_ALL_UNITS || client.player == null || client.level == null || !entity.hasAttached(UnitManager.UNIT)
				|| entity.distanceToSqr(client.player) > OUTLINE_RANGE * OUTLINE_RANGE) {
			return false;
		}
		// Outlines show through walls, so only draw them for units the player has a clear line to.
		// Checked once per tick per unit, not every frame.
		long now = client.level.getGameTime();
		long[] cached = LINE_OF_SIGHT.get(entity.getId());
		if (cached == null || cached[0] != now) {
			cached = new long[] {now, client.player.hasLineOfSight(entity) ? 1 : 0};
			LINE_OF_SIGHT.put(entity.getId(), cached);
		}
		return cached[1] == 1;
	}

	/** The client's view of {@link UnitCommands#canCommand}: your faction's unit, and you wear its crown. */
	public static boolean canCommand(Player player, Entity entity) {
		UnitData data = entity.getAttached(UnitManager.UNIT);
		return data != null && FactionClientState.mine().filter(f -> f.id().equals(data.faction())).isPresent()
				&& UnitCommands.wearsCrownFor(player, data);
	}

	/** How many selected units are loaded here (for the command menu). */
	public static int selectedCount() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? 0 : (int) SELECTED.stream().filter(id -> {
			Entity entity = client.level.getEntity(id);
			return entity != null && entity.isAlive();
		}).count();
	}

	/** How many of your commandable units are within command range (for the command menu). */
	public static int nearbyCount() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return 0;
		}
		List<Mob> nearby = client.level.getEntitiesOfClass(Mob.class, client.player.getBoundingBox().inflate(UnitCommands.COMMAND_RANGE),
				mob -> mob.isAlive() && canCommand(client.player, mob) && mob.distanceTo(client.player) <= UnitCommands.COMMAND_RANGE);
		return nearby.size();
	}
}

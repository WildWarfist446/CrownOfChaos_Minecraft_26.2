package com.crownofchaos.ability;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import com.crownofchaos.network.CrownAbilityKeyPayload;
import com.crownofchaos.network.RiptidePayload;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

/**
 * Maps each crown to its primary and secondary abilities and handles the ability keys sent
 * from the client.
 *
 * <p>Primary abilities use the crown's item cooldown, so it shows as a sweep on the crown's
 * icon. Item cooldowns only allow one per item, so secondary abilities track their own
 * cooldown here instead.
 */
public final class CrownAbilities {
	/** Entity tag on every projectile and cloud a crown ability creates. Saved with the entity. */
	public static final String CROWN_ABILITY_TAG = "crownofchaos.crown_ability";

	/** Color for crown ability messages; matches the crowns' epic (purple) item names. */
	public static final ChatFormatting MESSAGE_COLOR = ChatFormatting.LIGHT_PURPLE;

	private static final Map<AbilitySlot, Map<Item, CrownAbility>> ABILITIES = new EnumMap<>(AbilitySlot.class);

	/** Players currently holding each ability key. Only touched on the server thread. */
	private static final Map<AbilitySlot, Set<UUID>> HOLDING_KEY = new EnumMap<>(AbilitySlot.class);

	/** Server tick at which each player's secondary ability is ready again, per crown. */
	private static final Map<UUID, Map<Item, Integer>> SECONDARY_READY_AT = new HashMap<>();

	static {
		for (AbilitySlot slot : AbilitySlot.values()) {
			ABILITIES.put(slot, new HashMap<>());
			HOLDING_KEY.put(slot, new HashSet<>());
		}
	}

	private CrownAbilities() {
	}

	/** Attaches a primary ability (default key R) to a crown. Call during mod initialization. */
	public static void register(Item crown, CrownAbility ability) {
		register(AbilitySlot.PRIMARY, crown, ability);
	}

	/** Attaches a secondary ability (default key G) to a crown. Call during mod initialization. */
	public static void registerSecondary(Item crown, CrownAbility ability) {
		register(AbilitySlot.SECONDARY, crown, ability);
	}

	private static void register(AbilitySlot slot, Item crown, CrownAbility ability) {
		if (ABILITIES.get(slot).putIfAbsent(crown, ability) != null) {
			throw new IllegalStateException("Crown already has a " + slot + " ability: " + crown);
		}
	}

	/** Whether the player is holding down the "Use Crown Ability" key right now. */
	public static boolean isHoldingAbilityKey(ServerPlayer player) {
		return isHoldingAbilityKey(player, AbilitySlot.PRIMARY);
	}

	/** Whether the player is holding down the given ability key right now. */
	public static boolean isHoldingAbilityKey(ServerPlayer player, AbilitySlot slot) {
		return HOLDING_KEY.get(slot).contains(player.getUUID());
	}

	public static void initialize() {
		register(ModItems.UNDER_CROWN, new VeinMinerAbility());
		register(ModItems.CROWN_OF_POSEIDON, new RiptideAbility());
		register(ModItems.CROWN_OF_NETHER, new FireChargeAbility());
		register(ModItems.CROWN_OF_DARKNESS, new SonicBoomAbility());
		register(ModItems.CROWN_OF_SUN, new WindChargeAbility());
		register(ModItems.CROWN_OF_WITHER, new WitherSkullAbility());
		register(ModItems.CROWN_OF_WINTER, new FrostWalkerAbility());
		register(ModItems.CROWN_OF_IRON, new ArrowVolleyAbility());
		register(ModItems.CROWN_OF_VOID, new EnderPearlAbility());

		registerSecondary(ModItems.CROWN_OF_SUN, new SunCycleAbility());
		registerSecondary(ModItems.CROWN_OF_VOID, new DragonBreathAbility());

		VeinMinerAbility.initialize();
		SonicBoomAbility.initialize();
		FireChargeAbility.initialize();
		FrostWalkerAbility.initialize();
		ArrowVolleyAbility.initialize();
		DragonBreathAbility.initialize();

		PayloadTypeRegistry.serverboundPlay().register(CrownAbilityKeyPayload.TYPE, CrownAbilityKeyPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RiptidePayload.TYPE, RiptidePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CrownAbilityKeyPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Set<UUID> holding = HOLDING_KEY.get(payload.slot());
			if (payload.pressed()) {
				holding.add(player.getUUID());
				tryActivate(player, payload.slot());
			} else {
				holding.remove(player.getUUID());
			}
		});
		// Crown abilities never hurt the wearer's own pets.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(CrownEffects.isPetOf(entity, source.getEntity()) && isFromCrownAbility(source)));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			for (Set<UUID> holding : HOLDING_KEY.values()) {
				holding.remove(handler.getPlayer().getUUID());
			}
		});
	}

	/**
	 * Whether damage came from a crown ability: a crown-made projectile, cloud or explosion, the
	 * Crown of Darkness's sonic boom, or the Crown of Poseidon's Riptide spin attack.
	 */
	private static boolean isFromCrownAbility(DamageSource source) {
		if (source.getDirectEntity() != null && source.getDirectEntity().entityTags().contains(CROWN_ABILITY_TAG)) {
			return true;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker)) {
			return false;
		}
		return source.is(DamageTypes.SONIC_BOOM) && CrownEffects.isWearing(attacker, ModItems.CROWN_OF_DARKNESS)
				|| attacker.isAutoSpinAttack() && CrownEffects.isWearing(attacker, ModItems.CROWN_OF_POSEIDON);
	}

	private static void tryActivate(ServerPlayer player, AbilitySlot slot) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}

		ItemStack crown = player.getItemBySlot(EquipmentSlot.HEAD);
		// Netherite crowns have the same abilities as the crown they were upgraded from.
		Item baseCrown = ModItems.baseCrown(crown.getItem());
		// The Golden Crown is the plain base crown: the keys simply do nothing.
		if (baseCrown == ModItems.GOLDEN_CROWN) {
			return;
		}

		CrownAbility ability = ABILITIES.get(slot).get(baseCrown);
		if (ability == null) {
			String message = crown.isEmpty() ? "message.crownofchaos.ability.no_crown"
					: slot == AbilitySlot.PRIMARY ? "message.crownofchaos.ability.no_power"
					: "message.crownofchaos.ability.no_secondary";
			player.sendOverlayMessage(Component.translatable(message).withStyle(MESSAGE_COLOR));
			return;
		}

		if (slot == AbilitySlot.PRIMARY) {
			activatePrimary(player, crown, ability);
		} else {
			activateSecondary(player, crown, ability);
		}
	}

	private static void activatePrimary(ServerPlayer player, ItemStack crown, CrownAbility ability) {
		ItemCooldowns cooldowns = player.getCooldowns();
		if (cooldowns.isOnCooldown(crown)) {
			int secondsLeft = Mth.ceil(cooldowns.getCooldownPercent(crown, 0.0F) * ability.cooldownTicks() / 20.0F);
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.cooldown", secondsLeft).withStyle(MESSAGE_COLOR));
			return;
		}

		if (ability.activate(player, crown) && ability.cooldownTicks() > 0) {
			cooldowns.addCooldown(crown, ability.cooldownTicks());
		}
	}

	private static void activateSecondary(ServerPlayer player, ItemStack crown, CrownAbility ability) {
		int now = player.level().getServer().getTickCount();
		Map<Item, Integer> readyAt = SECONDARY_READY_AT.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
		Integer ready = readyAt.get(ModItems.baseCrown(crown.getItem()));
		// Ignore stale entries from before a world reload, when the server's tick counter restarts.
		if (ready != null && ready > now && ready - now <= ability.cooldownTicks()) {
			int secondsLeft = Mth.ceil((ready - now) / 20.0F);
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.secondary_cooldown", secondsLeft).withStyle(MESSAGE_COLOR));
			return;
		}

		if (ability.activate(player, crown) && ability.cooldownTicks() > 0) {
			readyAt.put(ModItems.baseCrown(crown.getItem()), now + ability.cooldownTicks());
		}
	}
}

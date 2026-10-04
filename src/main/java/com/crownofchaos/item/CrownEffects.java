package com.crownofchaos.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.crownofchaos.CrownOfChaos;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Grants each crown's potion effects while it is worn on the head. Crown abilities that
 * aren't potion effects live in mixins: Under Crown Fortune in
 * {@link com.crownofchaos.mixin.ApplyBonusCountMixin}, Crown of Poseidon damage in
 * {@link com.crownofchaos.mixin.LivingEntityMixin}. Crown of Darkness on-hit Darkness is
 * handled here in {@link #initialize()}.
 */
public final class CrownEffects {
	/** Fortune level granted to block drops while the Under Crown is worn. */
	public static final int UNDER_CROWN_FORTUNE_LEVEL = 3;

	/** Damage multiplier for attacks made while wearing the Crown of Poseidon in water. */
	public static final float POSEIDON_CROWN_WATER_DAMAGE_MULTIPLIER = 1.5F;

	/** Darkness given to anything the Crown of Darkness wearer hits (20 seconds). */
	public static final int DARKNESS_ON_HIT_TICKS = 400;

	/** Crown of Nether melee hits burn like Fire Aspect II: 4 seconds per level. */
	public static final float NETHER_CROWN_FIRE_ASPECT_SECONDS = 8.0F;

	/** Crown of Nether arrows are lit like Flame arrows, which burn for 100 seconds. */
	public static final float NETHER_CROWN_FLAME_SECONDS = 100.0F;

	/** Blocks the Crown of Sun gives Swiftness on: sand, sandstone and their slabs, stairs and walls. */
	public static final TagKey<Block> SUN_CROWN_SPEED_BLOCKS =
			TagKey.create(Registries.BLOCK, CrownOfChaos.id("crown_of_sun_speed_blocks"));

	/** Blocks the Crown of Winter gives Swiftness on: snow, snow blocks, powder snow and every kind of ice. */
	public static final TagKey<Block> WINTER_CROWN_SPEED_BLOCKS =
			TagKey.create(Registries.BLOCK, CrownOfChaos.id("crown_of_winter_speed_blocks"));

	/** Crowns that give Swiftness while standing on certain blocks. */
	private static final Map<Item, TagKey<Block>> TERRAIN_SPEED_BLOCKS = Map.of(
			ModItems.CROWN_OF_SUN, SUN_CROWN_SPEED_BLOCKS,
			ModItems.CROWN_OF_WINTER, WINTER_CROWN_SPEED_BLOCKS);

	/** Swiftness II, the strongest level potions and beacons give. */
	private static final CrownEffect TERRAIN_SWIFTNESS = new CrownEffect(MobEffects.SPEED, 1);

	/** Keeps Swiftness for 1 second after stepping off, so it doesn't flicker off while jumping. */
	private static final int TERRAIN_GRACE_TICKS = 20;

	/** Last server tick each wearer stood on their crown's Swiftness blocks. */
	private static final Map<UUID, Integer> LAST_ON_TERRAIN = new HashMap<>();

	/**
	 * Extra freezing a Crown of Winter hit adds on top of being fully frozen. Freezing thaws at
	 * 2 per tick, so the target stays fully frozen for 3.5 seconds, then thaws over 3.5 more.
	 */
	public static final int WINTER_CROWN_EXTRA_FREEZE_TICKS = 140;

	/** Crown of Wither melee hits give Wither I for 10 seconds, like a Wither Skeleton's. */
	public static final int WITHER_CROWN_WITHER_TICKS = 200;

	/** Damage multiplier for arrows a Crown of Iron wearer fires from a bow or crossbow. */
	public static final float IRON_CROWN_ARROW_DAMAGE_MULTIPLIER = 1.5F;

	// Amplifiers are zero-based: 0 = level I, 1 = level II.
	private static final Map<Item, List<CrownEffect>> EFFECTS = Map.of(
			ModItems.UNDER_CROWN, List.of(
					new CrownEffect(MobEffects.HASTE, 1),
					new CrownEffect(MobEffects.NIGHT_VISION, 0),
					new CrownEffect(MobEffects.HERO_OF_THE_VILLAGE, 0)),
			ModItems.CROWN_OF_POSEIDON, List.of(
					new CrownEffect(MobEffects.WATER_BREATHING, 0),
					new CrownEffect(MobEffects.DOLPHINS_GRACE, 0),
					new CrownEffect(MobEffects.NIGHT_VISION, 0)),
			ModItems.CROWN_OF_NETHER, List.of(
					new CrownEffect(MobEffects.FIRE_RESISTANCE, 0)),
			ModItems.CROWN_OF_DARKNESS, List.of(
					new CrownEffect(MobEffects.NIGHT_VISION, 0)));

	private static final int CHECK_INTERVAL_TICKS = 10;

	private CrownEffects() {
	}

	public static boolean isWearing(LivingEntity entity, Item crown) {
		return ModItems.baseCrown(entity.getItemBySlot(EquipmentSlot.HEAD).getItem()) == crown;
	}

	public static void initialize() {
		// Crown of Nether, Fire Aspect: direct melee hits set the target on fire.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& source.isDirect()
					&& !source.is(DamageTypes.THORNS)
					&& source.getEntity() instanceof LivingEntity attacker
					&& attacker != target
					&& isWearing(attacker, ModItems.CROWN_OF_NETHER)) {
				target.igniteForSeconds(NETHER_CROWN_FIRE_ASPECT_SECONDS);
			}
		});

		// Crown of Wither: direct melee hits inflict Wither, like a Wither Skeleton's.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& source.isDirect()
					&& !source.is(DamageTypes.THORNS)
					&& source.getEntity() instanceof LivingEntity attacker
					&& attacker != target
					&& isWearing(attacker, ModItems.CROWN_OF_WITHER)) {
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_CROWN_WITHER_TICKS), attacker);
			}
		});

		// Crown of Winter: every hit (melee or projectiles the wearer fires) freezes the target,
		// like being stuck in powder snow.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& source.getEntity() instanceof LivingEntity attacker
					&& attacker != target
					&& target.canFreeze()
					&& isWearing(attacker, ModItems.CROWN_OF_WINTER)) {
				int frozen = target.getTicksRequiredToFreeze() + WINTER_CROWN_EXTRA_FREEZE_TICKS;
				target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozen));
			}
		});

		// Crown of Nether, Flame: arrows the wearer fires are lit as they leave the bow, so they
		// burn, set targets on fire and light TNT exactly like Flame arrows.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof AbstractArrow arrow
					&& !(entity instanceof ThrownTrident)
					&& arrow.getDeltaMovement().lengthSqr() > 0.0
					&& arrow.getOwner() instanceof LivingEntity shooter
					&& isWearing(shooter, ModItems.CROWN_OF_NETHER)) {
				arrow.igniteForSeconds(NETHER_CROWN_FLAME_SECONDS);
			}
		});

		// Crown of Darkness: hits (including the wearer's arrows and tridents) inflict the
		// Warden's Darkness on the target.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& source.getEntity() instanceof LivingEntity attacker
					&& attacker != target
					&& isWearing(attacker, ModItems.CROWN_OF_DARKNESS)) {
				target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_ON_HIT_TICKS, 0, false, false), attacker);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_ON_TERRAIN.remove(handler.getPlayer().getUUID()));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Item worn = ModItems.baseCrown(player.getItemBySlot(EquipmentSlot.HEAD).getItem());
				List<CrownEffect> active = EFFECTS.getOrDefault(worn, List.of());
				TagKey<Block> speedBlocks = TERRAIN_SPEED_BLOCKS.get(worn);
				if (speedBlocks != null && isOnTerrain(player, speedBlocks, server.getTickCount())) {
					active = new ArrayList<>(active);
					active.add(TERRAIN_SWIFTNESS);
				}

				// Remove effects from crowns that are no longer worn, e.g. after swapping crowns.
				for (Map.Entry<Item, List<CrownEffect>> entry : EFFECTS.entrySet()) {
					if (entry.getKey() != worn) {
						removeCrownEffects(player, entry.getValue(), active);
					}
				}
				removeCrownEffects(player, List.of(TERRAIN_SWIFTNESS), active);

				apply(player, active);

				// Crown of Wither immunity also clears Wither the wearer already had.
				if (worn == ModItems.CROWN_OF_WITHER && player.hasEffect(MobEffects.WITHER)) {
					player.removeEffect(MobEffects.WITHER);
				}
			}
		});
	}

	private static boolean isOnTerrain(ServerPlayer player, TagKey<Block> blocks, int tick) {
		if (player.getBlockStateOn().is(blocks)) {
			LAST_ON_TERRAIN.put(player.getUUID(), tick);
			return true;
		}
		Integer last = LAST_ON_TERRAIN.get(player.getUUID());
		return last != null && tick - last <= TERRAIN_GRACE_TICKS;
	}

	private static void apply(ServerPlayer player, List<CrownEffect> effects) {
		for (CrownEffect crownEffect : effects) {
			MobEffectInstance current = player.getEffect(crownEffect.effect());
			if (current != null && isCrownEffect(current) && current.getAmplifier() == crownEffect.amplifier()) {
				continue;
			}
			// Don't overwrite a stronger effect from another source, e.g. a Haste II beacon.
			if (current != null && !isCrownEffect(current) && current.getAmplifier() > crownEffect.amplifier()) {
				continue;
			}
			player.addEffect(crownEffect.instance());
		}
	}

	private static void removeCrownEffects(ServerPlayer player, List<CrownEffect> effects, List<CrownEffect> keep) {
		for (CrownEffect crownEffect : effects) {
			if (keep.stream().anyMatch(k -> k.effect().equals(crownEffect.effect()))) {
				continue;
			}
			MobEffectInstance current = player.getEffect(crownEffect.effect());
			if (current != null && isCrownEffect(current)) {
				player.removeEffect(crownEffect.effect());
			}
		}
	}

	// Crown effects are the only ones that are both infinite and ambient, so this tells them
	// apart from potions and beacons, which are left untouched when the crown comes off.
	private static boolean isCrownEffect(MobEffectInstance instance) {
		return instance.isInfiniteDuration() && instance.isAmbient();
	}

	private record CrownEffect(Holder<MobEffect> effect, int amplifier) {
		MobEffectInstance instance() {
			return new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true);
		}
	}
}

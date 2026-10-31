package com.crownofchaos.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.crownofchaos.CrownOfChaos;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
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

	/** Blocks the Crown of Void gives Swiftness on: End blocks like end stone, purpur and obsidian. */
	public static final TagKey<Block> VOID_CROWN_SPEED_BLOCKS =
			TagKey.create(Registries.BLOCK, CrownOfChaos.id("crown_of_void_speed_blocks"));

	/** Crowns that give Swiftness while standing on certain blocks. */
	private static final Map<Item, TagKey<Block>> TERRAIN_SPEED_BLOCKS = Map.of(
			ModItems.CROWN_OF_SUN, SUN_CROWN_SPEED_BLOCKS,
			ModItems.CROWN_OF_WINTER, WINTER_CROWN_SPEED_BLOCKS,
			ModItems.CROWN_OF_VOID, VOID_CROWN_SPEED_BLOCKS);

	/** A Crown of Void wearer who gets hit teleports to a safe spot up to this many blocks away. */
	private static final double VOID_CROWN_TELEPORT_RANGE = 5.0;

	/** Minimum time between Crown of Void teleports (5 seconds), so every hit doesn't bounce them around. */
	private static final int VOID_CROWN_TELEPORT_COOLDOWN_TICKS = 100;
	/** Crown of Void: while gliding with an elytra, your velocity is multiplied by this every tick (3% faster; the elytra's drag takes back about 1%)... */
	public static final double VOID_CROWN_ELYTRA_SPEED_BONUS = 1.03;
	/** ...until you reach this speed in blocks per tick (3.0 is 60 blocks a second; a firework boost is about 1.6). */
	public static final double VOID_CROWN_ELYTRA_MAX_SPEED = 3.0;
	/** Cruising: a gliding wearer who is already moving never slows below this (blocks per tick, 1.4 is 28 blocks a second), speeding up to it by at most 5% a tick. */
	public static final double VOID_CROWN_ELYTRA_CRUISE_SPEED = 1.4;
	/** The speed below which the cruise speed is not applied, so taking off and stalling are left alone. */
	public static final double VOID_CROWN_ELYTRA_CRUISE_MIN_START = 0.5;

	/** Server tick of each Crown of Void wearer's last teleport. */
	private static final Map<UUID, Integer> LAST_VOID_TELEPORT = new HashMap<>();

	/** Regeneration II, the strongest level potions give, for the 22 seconds a strong Regeneration potion lasts. */
	public static final int POSEIDON_BOTTLE_REGENERATION_AMPLIFIER = 1;
	public static final int POSEIDON_BOTTLE_REGENERATION_TICKS = 22 * 20;

	/** Regeneration I, which the Crown of Poseidon grants while its wearer is in water. */
	private static final CrownEffect POSEIDON_WATER_REGENERATION = new CrownEffect(MobEffects.REGENERATION, 0);

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
			// The Crown of Chaos's one potion-style power: the Crown of Nether's fire and lava immunity.
			ModItems.CROWN_OF_CHAOS, List.of(
					new CrownEffect(MobEffects.FIRE_RESISTANCE, 0)),
			ModItems.CROWN_OF_DARKNESS, List.of(
					new CrownEffect(MobEffects.NIGHT_VISION, 0)));

	private static final int CHECK_INTERVAL_TICKS = 2;

	private CrownEffects() {
	}

	/** Whether the entity is an animal tamed by the given player or mob: wolves, cats, horses and so on. */
	public static boolean isPetOf(Entity entity, @Nullable Entity owner) {
		return owner != null && entity instanceof OwnableEntity pet && pet.getOwner() == owner;
	}

	public static boolean isWearing(LivingEntity entity, Item crown) {
		return ModItems.baseCrown(entity.getItemBySlot(EquipmentSlot.HEAD).getItem()) == crown;
	}

	/**
	 * Whether the entity wears the crown, or the Crown of Chaos, which has every crown's immunities
	 * and the Crown of Void's teleport dodge. Use this for those powers only; the other powers
	 * (on-hit effects, abilities) belong to their own crown.
	 */
	public static boolean isWearingOrChaos(LivingEntity entity, Item crown) {
		return isWearing(entity, crown) || isWearing(entity, ModItems.CROWN_OF_CHAOS);
	}

	public static void initialize() {
		// Crown of Nether, Fire Aspect: direct melee hits set the target on fire.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& source.isDirect()
					&& !source.is(DamageTypes.THORNS)
					&& source.getEntity() instanceof LivingEntity attacker
					&& attacker != target
					&& !isPetOf(target, attacker)
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
					&& !isPetOf(target, attacker)
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
					&& !isPetOf(target, attacker)
					&& isWearing(attacker, ModItems.CROWN_OF_WINTER)) {
				int frozen = target.getTicksRequiredToFreeze() + WINTER_CROWN_EXTRA_FREEZE_TICKS;
				target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozen));
			}
		});

		// Crown of Void: getting hit teleports the wearer to a safe spot nearby, like an Enderman,
		// sidestepping their attacker and turning to face it.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& target.isAlive()
					&& target instanceof ServerPlayer player
					// Not from landing an ender pearl, which would undo every throw, or from the void.
					&& !source.is(DamageTypes.ENDER_PEARL)
					&& !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
					&& isWearingOrChaos(player, ModItems.CROWN_OF_VOID)) {
				// The one who hit them: for arrows and other projectiles, whoever fired them.
				Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
				voidCrownTeleport(player, attacker == player ? null : attacker);
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
					&& !isPetOf(target, attacker)
					&& isWearing(attacker, ModItems.CROWN_OF_DARKNESS)) {
				target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_ON_HIT_TICKS, 0, false, false), attacker);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			LAST_ON_TERRAIN.remove(handler.getPlayer().getUUID());
			LAST_VOID_TELEPORT.remove(handler.getPlayer().getUUID());
		});

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
				if (worn == ModItems.CROWN_OF_POSEIDON && player.isInWater()) {
					active = new ArrayList<>(active);
					active.add(POSEIDON_WATER_REGENERATION);
				}

				// Remove effects from crowns that are no longer worn, e.g. after swapping crowns.
				for (Map.Entry<Item, List<CrownEffect>> entry : EFFECTS.entrySet()) {
					if (entry.getKey() != worn) {
						removeCrownEffects(player, entry.getValue(), active);
					}
				}
				removeCrownEffects(player, List.of(TERRAIN_SWIFTNESS, POSEIDON_WATER_REGENERATION), active);

				apply(player, active);

				// Crown of Wither immunity also clears Wither the wearer already had.
				if ((worn == ModItems.CROWN_OF_WITHER || worn == ModItems.CROWN_OF_CHAOS) && player.hasEffect(MobEffects.WITHER)) {
					player.removeEffect(MobEffects.WITHER);
				}
			}
		});
	}

	/**
	 * Teleports a Crown of Void wearer to a safe spot nearby. If something hit them, they
	 * sidestep up to 5 blocks to its left or right (not straight back, which would leave a
	 * ranged attacker a clean shot) and end up facing it. Otherwise they teleport to a random
	 * safe spot within range, like chorus fruit (see vanilla {@code TeleportRandomlyConsumeEffect}).
	 */
	private static void voidCrownTeleport(ServerPlayer player, @Nullable Entity attacker) {
		ServerLevel level = (ServerLevel) player.level();
		int now = level.getServer().getTickCount();
		Integer last = LAST_VOID_TELEPORT.get(player.getUUID());
		// Ignore stale entries from before a world reload, when the server's tick counter restarts.
		if (last != null && now >= last && now - last < VOID_CROWN_TELEPORT_COOLDOWN_TICKS) {
			return;
		}

		if (player.isPassenger()) {
			player.stopRiding();
		}

		Vec3 oldPos = player.position();
		boolean teleported = attacker != null && attacker.level() == level
				? sidestep(player, attacker)
				: teleportRandomly(player, level);
		if (!teleported) {
			return;
		}

		level.gameEvent(GameEvent.TELEPORT, oldPos, GameEvent.Context.of(player));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.resetFallDistance();
		LAST_VOID_TELEPORT.put(player.getUUID(), now);

		if (attacker != null && attacker.level() == level) {
			faceTowards(player, attacker);
		}
	}

	/** Teleports to a safe spot to the attacker's left or right, trying farther spots first. */
	private static boolean sidestep(ServerPlayer player, Entity attacker) {
		double dx = attacker.getX() - player.getX();
		double dz = attacker.getZ() - player.getZ();
		// Straight at the attacker, flattened; if they're right on top of us, use where we're facing.
		double towardsAttacker = dx * dx + dz * dz > 1.0E-4 ? Math.atan2(dz, dx) : Math.toRadians(player.getYRot() + 90.0F);

		double firstSide = player.getRandom().nextBoolean() ? 1.0 : -1.0;
		for (double distance = VOID_CROWN_TELEPORT_RANGE; distance >= 2.0; distance -= 1.0) {
			for (double side : new double[] {firstSide, -firstSide}) {
				for (int attempt = 0; attempt < 3; attempt++) {
					// 90 degrees off the line to the attacker, give or take up to 25.
					double jitter = Math.toRadians((player.getRandom().nextDouble() - 0.5) * 50.0);
					double angle = towardsAttacker + side * Math.PI / 2.0 + jitter;
					double x = player.getX() + Math.cos(angle) * distance;
					double z = player.getZ() + Math.sin(angle) * distance;
					// Start a little higher so small steps up still count; randomTeleport drops to the ground.
					double y = player.getY() + 2.0;
					if (player.randomTeleport(x, y, z, true)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean teleportRandomly(ServerPlayer player, ServerLevel level) {
		double diameter = VOID_CROWN_TELEPORT_RANGE * 2.0;
		for (int attempt = 0; attempt < 16; attempt++) {
			double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * diameter;
			double y = Mth.clamp(player.getY() + (player.getRandom().nextDouble() - 0.5) * diameter,
					level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
			double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * diameter;
			if (player.randomTeleport(x, y, z, true)) {
				return true;
			}
		}
		return false;
	}

	/** Turns the player to look at the entity's eyes. */
	private static void faceTowards(ServerPlayer player, Entity target) {
		Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
		double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		float yaw = Mth.wrapDegrees((float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F);
		float pitch = Mth.wrapDegrees((float) -(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG));
		player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
		player.setYHeadRot(yaw);
	}

	/**
	 * Whether the player is standing on (or, after the grace period, just stepped off) one of
	 * the blocks. Also checks the block at their feet: a single layer of snow has no collision,
	 * so they actually stand on the block beneath it.
	 */
	private static boolean isOnTerrain(ServerPlayer player, TagKey<Block> blocks, int tick) {
		if (player.getBlockStateOn().is(blocks) || player.level().getBlockState(player.blockPosition()).is(blocks)) {
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

package com.crownofchaos.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Iron ability: rains a volley of 24 arrows down on the mob or player the wearer is
 * looking at, within 20 blocks. The arrows appear 3 blocks above the target's head over 1.5
 * seconds, ahead of a moving target. About half are aimed at its body and gently correct
 * course while falling; the rest scatter around it like a rain of arrows. A target that keeps
 * moving can dodge some. Volley arrows hit 25% harder than normal arrows, ignore the target's brief
 * invulnerability after each hit (see {@link com.crownofchaos.mixin.AbstractArrowMixin}) and
 * can't be picked up.
 */
public final class ArrowVolleyAbility implements CrownAbility {
	/** Entity tag marking volley arrows. Saved with the entity. */
	public static final String ARROW_TAG = "crownofchaos.iron_crown_volley";

	private static final double RANGE = 20.0;
	private static final int ARROW_COUNT = 24;
	private static final double HEIGHT_ABOVE_HEAD = 3.0;
	/** Chance for each arrow to be aimed at the target's body rather than scattered around it. */
	private static final float AIMED_CHANCE = 0.5F;
	/** How far from the target's center aimed arrows aim, as a fraction of its width. */
	private static final double AIMED_SPREAD = 0.35;
	/** Scattered arrows land anywhere within this many blocks of the target's center. */
	private static final double SCATTER_SPREAD = 1.5;
	/** Most an aimed arrow can correct its course sideways per tick while falling. */
	private static final double MAX_STEER = 0.12;
	/** Ticks to keep steering after the last arrow spawns, while it's still falling. */
	private static final int STEER_TICKS_AFTER_LAST = 10;
	/** The arrows are spread out over this many ticks (1.5 seconds). */
	private static final int DURATION_TICKS = 30;
	private static final double FALL_SPEED = 1.0;
	/** 25% more than a normal arrow's base damage of 2. */
	private static final double ARROW_BASE_DAMAGE = 2.5;

	/** 10 seconds, counted from when the volley starts. */
	private static final int COOLDOWN_TICKS = 200;

	/** Volleys still dropping arrows. Only touched on the server thread. */
	private static final List<Volley> VOLLEYS = new ArrayList<>();

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		LivingEntity target = findTarget(player);
		if (target == null) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.arrow_volley.no_target")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		VOLLEYS.add(new Volley(player, target));
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(ArrowVolleyAbility::tickVolleys);
	}

	private static void tickVolleys(MinecraftServer server) {
		Iterator<Volley> iterator = VOLLEYS.iterator();
		while (iterator.hasNext()) {
			Volley volley = iterator.next();
			// Stop if the shooter or target is gone, or they're now in different dimensions.
			if (volley.shooter.isRemoved() || !volley.target.isAlive() || volley.target.level() != volley.shooter.level()) {
				iterator.remove();
				continue;
			}

			// Track how far the target moved this tick, so arrows can lead it. Measured from its
			// position because the server doesn't know a player's velocity reliably.
			Vec3 position = volley.target.position();
			volley.velocity = volley.lastPosition == null ? Vec3.ZERO : position.subtract(volley.lastPosition);
			volley.lastPosition = position;

			// Spread the arrows evenly over the volley's duration.
			int due = Math.min((volley.ticks + 1) * ARROW_COUNT / DURATION_TICKS, ARROW_COUNT);
			while (volley.fired < due) {
				volley.arrows.add(spawnArrow(volley));
				volley.fired++;
			}
			steerArrows(volley);
			if (++volley.ticks >= DURATION_TICKS + STEER_TICKS_AFTER_LAST) {
				iterator.remove();
			}
		}
	}

	private static VolleyArrow spawnArrow(Volley volley) {
		ServerLevel level = (ServerLevel) volley.target.level();
		RandomSource random = level.getRandom();
		// Aimed arrows pick their own spot on the target's body; scattered ones land around it.
		boolean aimed = random.nextFloat() < AIMED_CHANCE;
		double spread = aimed ? AIMED_SPREAD * volley.target.getBbWidth() : SCATTER_SPREAD;
		double angle = random.nextDouble() * Math.PI * 2.0;
		double distance = Math.sqrt(random.nextDouble()) * spread;
		double offsetX = Math.cos(angle) * distance;
		double offsetZ = Math.sin(angle) * distance;
		// Spawn ahead of a moving target by the distance it covers while the arrow falls.
		double fallTicks = (HEIGHT_ABOVE_HEAD + volley.target.getBbHeight() / 2.0) / FALL_SPEED;
		double x = volley.target.getX() + volley.velocity.x * fallTicks + offsetX;
		double y = volley.target.getY() + volley.target.getBbHeight() + HEIGHT_ABOVE_HEAD;
		double z = volley.target.getZ() + volley.velocity.z * fallTicks + offsetZ;

		Arrow arrow = new Arrow(level, x, y, z, new ItemStack(Items.ARROW), null);
		arrow.setOwner(volley.shooter);
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		arrow.setBaseDamage(ARROW_BASE_DAMAGE);
		arrow.setDeltaMovement(0.0, -FALL_SPEED, 0.0);
		arrow.addTag(ARROW_TAG);
		level.addFreshEntity(arrow);

		if (volley.fired % 8 == 0) {
			level.playSound(null, x, y, z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
					1.0F / (random.nextFloat() * 0.4F + 1.2F));
		}
		return new VolleyArrow(arrow, offsetX, offsetZ, aimed);
	}

	/**
	 * Gently corrects each falling aimed arrow toward its spot on the target, leading the target
	 * by how far it's moving each tick. The correction is small, so a moving target can still
	 * outrun it. Scattered arrows fall straight.
	 */
	private static void steerArrows(Volley volley) {
		LivingEntity target = volley.target;
		double targetMiddle = target.getY() + target.getBbHeight() / 2.0;
		Iterator<VolleyArrow> iterator = volley.arrows.iterator();
		while (iterator.hasNext()) {
			VolleyArrow volleyArrow = iterator.next();
			Arrow arrow = volleyArrow.arrow;
			Vec3 movement = arrow.getDeltaMovement();
			double heightLeft = arrow.getY() - targetMiddle;
			// Done once it has hit something, landed, or dropped past the target.
			if (!volleyArrow.aimed || arrow.isRemoved() || movement.y >= 0.0 || heightLeft <= 0.0) {
				iterator.remove();
				continue;
			}

			double ticksLeft = Math.max(1.0, heightLeft / -movement.y);
			double aimX = target.getX() + volley.velocity.x * ticksLeft + volleyArrow.offsetX;
			double aimZ = target.getZ() + volley.velocity.z * ticksLeft + volleyArrow.offsetZ;
			double steerX = Mth.clamp((aimX - arrow.getX()) / ticksLeft, -MAX_STEER, MAX_STEER);
			double steerZ = Mth.clamp((aimZ - arrow.getZ()) / ticksLeft, -MAX_STEER, MAX_STEER);
			arrow.setDeltaMovement(steerX, movement.y, steerZ);
			// Send the new heading to players so what they see matches where it hits.
			arrow.hurtMarked = true;
		}
	}

	/** The living entity under the player's crosshair, if it's in sight and within range. */
	private static LivingEntity findTarget(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(RANGE));

		BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}

		AABB searchArea = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchArea,
				entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(),
				eye.distanceToSqr(end));
		if (entityHit == null) {
			return null;
		}

		Entity hit = entityHit.getEntity();
		return hit instanceof LivingEntity living && player.distanceTo(living) <= RANGE ? living : null;
	}

	private static final class Volley {
		private final ServerPlayer shooter;
		private final LivingEntity target;
		private int ticks;
		private int fired;
		private Vec3 lastPosition;
		private Vec3 velocity = Vec3.ZERO;
		private final List<VolleyArrow> arrows = new ArrayList<>();

		private Volley(ServerPlayer shooter, LivingEntity target) {
			this.shooter = shooter;
			this.target = target;
		}
	}

	/** A falling volley arrow, the spot relative to the target it aims for, and whether it steers. */
	private record VolleyArrow(Arrow arrow, double offsetX, double offsetZ, boolean aimed) {
	}
}

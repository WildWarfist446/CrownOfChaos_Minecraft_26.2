package com.crownofchaos.unit;

import java.util.EnumSet;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The AI goals that carry out a unit's orders (see {@link UnitOrder}). Added to every unit when
 * it loads (see {@link UnitManager}). Mobs run by a "brain" (Piglins, Hoglins, Wardens) are
 * steered through their walk target instead of their path finder.
 */
final class UnitGoals {
	/** A sentry chases enemies up to this many blocks from its post, then gives up and goes back. */
	static final double SENTRY_LEASH = 16.0;
	/** A following unit teleports to its leader when it falls this far behind. */
	private static final double FOLLOW_TELEPORT_DISTANCE = 24.0;
	/** A following unit stops this close to its leader. */
	private static final double FOLLOW_STOP_DISTANCE = 3.5;
	private static final double WALK_SPEED = 1.0;
	private static final double FOLLOW_SPEED = 1.2;
	/** How long a Piglin unit stays angry at an enemy it was handed (30 seconds, like a provoked Piglin). */
	private static final long ANGER_TICKS = 600L;
	/** How long each refresh of a Warden unit's dig cooldown lasts (refreshed every half second). */
	private static final long WARDEN_DIG_COOLDOWN_TICKS = 1200L;

	private UnitGoals() {
	}

	/** Its current target, whether set by its goals or its brain. */
	static @Nullable LivingEntity targetOf(Mob mob) {
		if (mob.getTarget() != null) {
			return mob.getTarget();
		}
		return hasMemory(mob, MemoryModuleType.ATTACK_TARGET) ? mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) : null;
	}

	static void clearTarget(Mob mob) {
		mob.setTarget(null);
		if (hasMemory(mob, MemoryModuleType.ATTACK_TARGET)) {
			mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
		}
	}

	/** Whether the mob's brain has this memory at all; asking one that doesn't for it is an error. */
	private static boolean hasMemory(Mob mob, MemoryModuleType<?> memory) {
		return mob.getBrain().checkMemory(memory, MemoryStatus.REGISTERED);
	}

	/**
	 * Keeps a Warden unit from burrowing into the ground, which removes it: wild Wardens dig away
	 * whenever they have no dig cooldown, so units always get one.
	 */
	static void keepWardenFromDigging(Mob mob) {
		if (mob instanceof Warden) {
			mob.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, WARDEN_DIG_COOLDOWN_TICKS);
		}
	}

	/** Walks the unit toward a spot, by path finding or, for brain mobs, by setting its walk target. */
	private static void walkTo(Mob mob, Vec3 to, double speed, int closeEnough) {
		if (hasMemory(mob, MemoryModuleType.WALK_TARGET)) {
			mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(to, (float) speed, closeEnough));
		} else if (!mob.getNavigation().moveTo(to.x, to.y, to.z, speed)) {
			// Flying mobs like Ghasts have no path finder; steer them directly.
			mob.getMoveControl().setWantedPosition(to.x, to.y, to.z, speed);
		}
	}

	private static void stop(Mob mob) {
		mob.getNavigation().stop();
		if (hasMemory(mob, MemoryModuleType.WALK_TARGET)) {
			mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		}
	}

	/**
	 * Holds a sentry at its post, and keeps a following unit with its leader, whenever it isn't
	 * fighting. Takes over movement from the mob's own wandering while it runs.
	 */
	static final class OrdersGoal extends Goal {
		private final Mob mob;
		private int repathTicks;

		OrdersGoal(Mob mob) {
			this.mob = mob;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			UnitData data = this.mob.getAttached(UnitManager.UNIT);
			if (data == null || targetOf(this.mob) != null) {
				return false;
			}
			return data.order() == UnitOrder.SENTRY && data.post().isPresent()
					|| data.order() == UnitOrder.FOLLOW && this.leader(data) != null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.repathTicks = 0;
		}

		@Override
		public void stop() {
			UnitGoals.stop(this.mob);
		}

		@Override
		public void tick() {
			UnitData data = this.mob.getAttached(UnitManager.UNIT);
			if (data == null) {
				return;
			}
			if (data.order() == UnitOrder.SENTRY) {
				BlockPos post = data.post().orElseThrow();
				Vec3 spot = Vec3.atBottomCenterOf(post);
				if (this.mob.position().distanceToSqr(spot) > 1.5 * 1.5) {
					if (--this.repathTicks <= 0) {
						this.repathTicks = 10;
						walkTo(this.mob, spot, WALK_SPEED, 0);
					}
				} else {
					UnitGoals.stop(this.mob);
				}
				return;
			}
			Player leader = this.leader(data);
			if (leader == null) {
				return;
			}
			double distance = this.mob.distanceTo(leader);
			this.mob.getLookControl().setLookAt(leader, 10.0F, this.mob.getMaxHeadXRot());
			if (distance > FOLLOW_TELEPORT_DISTANCE) {
				this.mob.teleportTo(leader.getX(), leader.getY(), leader.getZ());
				UnitGoals.stop(this.mob);
			} else if (distance > FOLLOW_STOP_DISTANCE) {
				if (--this.repathTicks <= 0) {
					this.repathTicks = 10;
					walkTo(this.mob, leader.position(), FOLLOW_SPEED, 2);
				}
			} else {
				UnitGoals.stop(this.mob);
			}
		}

		private @Nullable Player leader(UnitData data) {
			Optional<Player> leader = data.leader().map(id -> this.mob.level().getPlayerByUUID(id));
			return leader.filter(player -> player.isAlive() && !player.isSpectator()).orElse(null);
		}
	}

	/**
	 * Drops a target the unit shouldn't be fighting (it left the territory, is now allowed, or a
	 * sentry's enemy ran too far from its post), so the unit goes back to its orders. For mobs
	 * run by a "brain" (Piglins, Hoglins, Wardens), which ignore target goals, it also finds
	 * enemies the faction rules say to fight and hands them over. Runs alongside everything else.
	 */
	static final class WatchGoal extends Goal {
		private final Mob mob;
		private int ticks;

		WatchGoal(Mob mob) {
			this.mob = mob;
			this.setFlags(EnumSet.noneOf(Goal.Flag.class));
		}

		@Override
		public boolean canUse() {
			return this.mob.hasAttached(UnitManager.UNIT);
		}

		@Override
		public void tick() {
			if (++this.ticks % 10 != 0 || !(this.mob.level() instanceof ServerLevel)) {
				return;
			}
			keepWardenFromDigging(this.mob);
			LivingEntity target = targetOf(this.mob);
			if (target != null && !UnitManager.isHostile(this.mob, target)) {
				clearTarget(this.mob);
			} else if (target == null && hasMemory(this.mob, MemoryModuleType.ATTACK_TARGET)) {
				this.findTargetForBrain();
			}
		}

		/** The nearest enemy it can see, within its follow range, given to its brain to fight. */
		private void findTargetForBrain() {
			double range = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
			// A fresh copy: the shared vanilla conditions mustn't be changed.
			LivingEntity enemy = ((ServerLevel) this.mob.level()).getNearestEntity(LivingEntity.class, TargetingConditions.forCombat().range(range)
					.selector((candidate, level) -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(candidate) && UnitManager.isHostile(this.mob, candidate)),
					this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(), this.mob.getBoundingBox().inflate(range));
			if (enemy == null || !this.mob.hasLineOfSight(enemy)) {
				return;
			}
			if (this.mob instanceof Warden warden) {
				// Wardens fight whoever they're angriest at; make this enemy that.
				warden.increaseAngerAt(enemy, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
			} else {
				// Piglins drop any target that isn't the one they'd pick themselves, and they pick
				// whoever they're angry at first, so make them angry at it too.
				if (hasMemory(this.mob, MemoryModuleType.ANGRY_AT)) {
					this.mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, enemy.getUUID(), ANGER_TICKS);
				}
				this.mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, enemy);
			}
		}
	}
}

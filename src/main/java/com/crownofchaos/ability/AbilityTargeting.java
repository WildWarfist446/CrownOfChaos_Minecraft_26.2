package com.crownofchaos.ability;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Finds what a player is aiming at, for targeted crown abilities.
 */
final class AbilityTargeting {
	private AbilityTargeting() {
	}

	/**
	 * The living entity under the player's crosshair, up to {@code range} blocks away along their
	 * line of sight, with nothing solid in between; or null if there isn't one.
	 */
	static @Nullable LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(range));

		BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}

		AABB searchArea = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchArea,
				entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(),
				eye.distanceToSqr(end));
		return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
	}
}

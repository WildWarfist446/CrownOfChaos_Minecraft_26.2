package com.crownofchaos.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Nether ability: shoots a Ghast fireball where the player is looking, without
 * needing a fire charge item. It explodes like a Ghast's but doesn't break blocks or start
 * fires (see {@link com.crownofchaos.mixin.LargeFireballMixin}).
 */
public final class FireChargeAbility implements CrownAbility {
	/** Entity tag marking fireballs shot by this ability. Saved with the entity. */
	public static final String FIREBALL_TAG = "crownofchaos.nether_crown_fireball";

	/** Same explosion power as a Ghast's fireball. */
	private static final int EXPLOSION_POWER = 1;

	/** 25% faster than a Ghast's fireball, whose acceleration is 0.1. */
	private static final double ACCELERATION = 0.125;

	/** 2 seconds between shots. */
	private static final int COOLDOWN_TICKS = 40;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		Vec3 look = player.getLookAngle();
		LargeFireball fireball = new LargeFireball(player.level(), player, look, EXPLOSION_POWER);
		// Spawn just in front of the player's eyes, like a thrown projectile.
		Vec3 start = player.getEyePosition().add(look.scale(0.5));
		fireball.setPos(start.x, start.y, start.z);
		fireball.accelerationPower = ACCELERATION;
		fireball.setDeltaMovement(look.normalize().scale(ACCELERATION));
		fireball.addTag(FIREBALL_TAG);
		player.level().addFreshEntity(fireball);

		player.level().playSound(null, player, SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F,
				(player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

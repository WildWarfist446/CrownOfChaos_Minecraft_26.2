package com.crownofchaos.ability;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Nether ability: shoots a Ghast fireball where the player is looking, without
 * needing a fire charge item. It explodes like a Ghast's but doesn't break blocks or start
 * fires (see {@link com.crownofchaos.mixin.LargeFireballMixin}). Its direct hit and blast deal
 * 50% more damage than a Ghast's (see {@link com.crownofchaos.mixin.LivingEntityMixin}) and
 * set whatever they damage on fire.
 */
public final class FireChargeAbility implements CrownAbility {
	/** Entity tag marking fireballs shot by this ability. Saved with the entity. */
	public static final String FIREBALL_TAG = "crownofchaos.nether_crown_fireball";

	/** Damage multiplier for the direct hit and the blast, compared to a Ghast's fireball. */
	public static final float DAMAGE_MULTIPLIER = 1.5F;

	/** How long things damaged by the fireball burn; same as being hit by a fire charge. */
	private static final float BURN_SECONDS = 5.0F;

	/** Same explosion power as a Ghast's fireball. */
	private static final int EXPLOSION_POWER = 1;

	/** 25% faster than a Ghast's fireball, whose acceleration is 0.1. */
	private static final double ACCELERATION = 0.125;

	/** 2 seconds between shots. */
	private static final int COOLDOWN_TICKS = 40;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		shoot(player);
		return true;
	}

	/** Shoots one fireball where the player is looking. */
	static void shoot(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		LargeFireball fireball = new LargeFireball(player.level(), player, look, EXPLOSION_POWER);
		// Spawn just in front of the player's eyes, like a thrown projectile.
		Vec3 start = player.getEyePosition().add(look.scale(0.5));
		fireball.setPos(start.x, start.y, start.z);
		fireball.accelerationPower = ACCELERATION;
		fireball.setDeltaMovement(look.normalize().scale(ACCELERATION));
		fireball.addTag(FIREBALL_TAG);
		fireball.addTag(CrownAbilities.CROWN_ABILITY_TAG);
		player.level().addFreshEntity(fireball);

		player.level().playSound(null, player, SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F,
				(player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	/** Whether the entity is a fireball shot by this ability. */
	public static boolean isCrownFireball(@Nullable Entity entity) {
		return entity instanceof LargeFireball && entity.entityTags().contains(FIREBALL_TAG);
	}

	static void initialize() {
		// Set anything the fireball damages on fire, except the shooter caught in their own blast.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0.0F && isCrownFireball(source.getDirectEntity()) && target != source.getEntity()) {
				target.igniteForSeconds(BURN_SECONDS);
			}
		});
	}
}

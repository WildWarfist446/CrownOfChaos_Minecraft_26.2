package com.crownofchaos.ability;

import com.crownofchaos.item.CrownEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * Crown of Darkness secondary ability: a shriek that gives every other player within 30
 * blocks the Warden's Darkness. Like the Warden, it only affects players, since Darkness only
 * changes what a player sees.
 */
public final class DarknessPulseAbility implements CrownAbility {
	private static final double RADIUS = 30.0;

	/** 30 seconds between uses. */
	private static final int COOLDOWN_TICKS = 600;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ServerLevel level = (ServerLevel) player.level();
		for (ServerPlayer other : level.players()) {
			if (other != player && !other.isSpectator() && other.distanceToSqr(player) <= RADIUS * RADIUS) {
				// Same as the Warden's: no particles, and the same length as the crown's on-hit Darkness.
				other.addEffect(new MobEffectInstance(MobEffects.DARKNESS, CrownEffects.DARKNESS_ON_HIT_TICKS, 0, false, false), player);
			}
		}

		level.playSound(null, player, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 2.0F, 1.0F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

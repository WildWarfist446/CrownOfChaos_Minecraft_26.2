package com.crownofchaos.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Crown of Void ability: throws an ender pearl, exactly like using the item (see vanilla
 * {@code EnderpearlItem#use}), without needing one.
 */
public final class EnderPearlAbility implements CrownAbility {
	/** 3 seconds between throws. */
	private static final int COOLDOWN_TICKS = 60;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		Projectile.spawnProjectileFromRotation(ThrownEnderpearl::new, level, new ItemStack(Items.ENDER_PEARL), player, 0.0F, 1.5F, 1.0F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

package com.crownofchaos.ability;

import com.crownofchaos.network.OreSensePayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Under Crown secondary ability: every ore within 16 blocks glows through walls for 40
 * seconds, outlined in its ore's color, until the time runs out or the ore is mined. Only the wearer sees the glow (see
 * {@code OreSenseClient} on the client).
 */
public final class OreSenseAbility implements CrownAbility {
	/** Ores this many blocks or fewer from the wearer in every direction are shown. */
	public static final int RADIUS = 16;
	/** 40 seconds. */
	public static final int DURATION_TICKS = 800;

	/** 60 seconds between uses: seeing every ore nearby is strong. */
	private static final int COOLDOWN_TICKS = 1200;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ServerPlayNetworking.send(player, new OreSensePayload(RADIUS, DURATION_TICKS));
		player.level().playSound(null, player, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

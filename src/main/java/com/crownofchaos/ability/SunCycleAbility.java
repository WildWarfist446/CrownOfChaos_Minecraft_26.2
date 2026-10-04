package com.crownofchaos.ability;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.item.ItemStack;

/**
 * Crown of Sun secondary ability: switches between day and night, the same as
 * {@code /time set day} or {@code /time set night}. Only works in dimensions with a day
 * cycle, like the Overworld. This changes the time for everyone in that dimension.
 */
public final class SunCycleAbility implements CrownAbility {
	/** 20 seconds between uses. */
	private static final int COOLDOWN_TICKS = 400;

	/** Length of a Minecraft day; the first half is daytime. */
	private static final long DAY_LENGTH = 24000L;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ServerLevel level = (ServerLevel) player.level();
		Optional<Holder<WorldClock>> clock = level.dimensionTypeRegistration().value().defaultClock();
		if (clock.isEmpty() || level.dimensionType().hasFixedTime()) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.sun_cycle.no_sky")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		ServerClockManager clocks = level.getServer().clockManager();
		boolean isDay = clocks.getTotalTicks(clock.get()) % DAY_LENGTH < DAY_LENGTH / 2;
		if (!clocks.moveToTimeMarker(clock.get(), isDay ? ClockTimeMarkers.NIGHT : ClockTimeMarkers.DAY)) {
			return false;
		}

		player.sendOverlayMessage(Component.translatable(isDay
				? "message.crownofchaos.ability.sun_cycle.night"
				: "message.crownofchaos.ability.sun_cycle.day").withStyle(CrownAbilities.MESSAGE_COLOR));
		level.playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, isDay ? 0.6F : 1.4F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

package com.crownofchaos.item;

import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Grants the Under Crown's effects while it is worn on the head. Fortune is handled
 * separately in {@link com.crownofchaos.mixin.ApplyBonusCountMixin}.
 */
public final class UnderCrownEffects {
	/** Fortune level granted to block drops while the crown is worn. */
	public static final int FORTUNE_LEVEL = 3;

	// Amplifiers are zero-based: 0 = level I, 1 = level II.
	private static final List<CrownEffect> EFFECTS = List.of(
			new CrownEffect(MobEffects.HASTE, 1),
			new CrownEffect(MobEffects.NIGHT_VISION, 0),
			new CrownEffect(MobEffects.HERO_OF_THE_VILLAGE, 0));

	private static final int CHECK_INTERVAL_TICKS = 10;

	private UnderCrownEffects() {
	}

	public static boolean isWearing(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.UNDER_CROWN;
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (isWearing(player)) {
					apply(player);
				} else {
					removeCrownEffects(player);
				}
			}
		});
	}

	private static void apply(ServerPlayer player) {
		for (CrownEffect crownEffect : EFFECTS) {
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

	private static void removeCrownEffects(ServerPlayer player) {
		for (CrownEffect crownEffect : EFFECTS) {
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

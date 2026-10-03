package com.crownofchaos.item;

import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * Grants each crown's potion effects while it is worn on the head. Crown abilities that
 * aren't potion effects live in mixins: Under Crown Fortune in
 * {@link com.crownofchaos.mixin.ApplyBonusCountMixin}, Crown of Poseidon damage in
 * {@link com.crownofchaos.mixin.LivingEntityMixin}.
 */
public final class CrownEffects {
	/** Fortune level granted to block drops while the Under Crown is worn. */
	public static final int UNDER_CROWN_FORTUNE_LEVEL = 3;

	/** Damage multiplier for attacks made while wearing the Crown of Poseidon in water. */
	public static final float POSEIDON_CROWN_WATER_DAMAGE_MULTIPLIER = 1.5F;

	// Amplifiers are zero-based: 0 = level I, 1 = level II.
	private static final Map<Item, List<CrownEffect>> EFFECTS = Map.of(
			ModItems.UNDER_CROWN, List.of(
					new CrownEffect(MobEffects.HASTE, 1),
					new CrownEffect(MobEffects.NIGHT_VISION, 0),
					new CrownEffect(MobEffects.HERO_OF_THE_VILLAGE, 0)),
			ModItems.CROWN_OF_POSEIDON, List.of(
					new CrownEffect(MobEffects.WATER_BREATHING, 0),
					new CrownEffect(MobEffects.DOLPHINS_GRACE, 0),
					new CrownEffect(MobEffects.NIGHT_VISION, 0)));

	private static final int CHECK_INTERVAL_TICKS = 10;

	private CrownEffects() {
	}

	public static boolean isWearing(LivingEntity entity, Item crown) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() == crown;
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Item worn = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
				List<CrownEffect> active = EFFECTS.getOrDefault(worn, List.of());

				// Remove effects from crowns that are no longer worn, e.g. after swapping crowns.
				for (Map.Entry<Item, List<CrownEffect>> entry : EFFECTS.entrySet()) {
					if (entry.getKey() != worn) {
						removeCrownEffects(player, entry.getValue(), active);
					}
				}

				apply(player, active);
			}
		});
	}

	private static void apply(ServerPlayer player, List<CrownEffect> effects) {
		for (CrownEffect crownEffect : effects) {
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

	private static void removeCrownEffects(ServerPlayer player, List<CrownEffect> effects, List<CrownEffect> keep) {
		for (CrownEffect crownEffect : effects) {
			if (keep.stream().anyMatch(k -> k.effect().equals(crownEffect.effect()))) {
				continue;
			}
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

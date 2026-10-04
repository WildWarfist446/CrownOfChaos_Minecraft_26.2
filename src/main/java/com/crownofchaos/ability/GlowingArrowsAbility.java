package com.crownofchaos.ability;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * Crown of Iron secondary ability: pressing the secondary ability key toggles Glowing Arrows
 * on or off. While it's on, anything hit by an arrow the wearer fires from a bow or crossbow
 * glows for 2 minutes, like a long-lasting Spectral Arrow hit. Arrow Volley arrows don't count,
 * and the wearer's own pets never glow.
 */
public final class GlowingArrowsAbility implements CrownAbility {
	/** 2 minutes. */
	private static final int GLOWING_TICKS = 2400;

	private static final ToggleState TOGGLE = new ToggleState("glowing_arrows");

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		TOGGLE.toggle(player);
		// Toggling isn't a use of the ability, so no cooldown.
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	static void initialize() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0.0F
					&& source.is(DamageTypes.ARROW)
					&& source.getDirectEntity() != null
					&& !source.getDirectEntity().entityTags().contains(ArrowVolleyAbility.ARROW_TAG)
					&& source.getEntity() instanceof ServerPlayer shooter
					&& shooter != target
					&& TOGGLE.isEnabled(shooter)
					&& !CrownEffects.isPetOf(target, shooter)
					&& CrownEffects.isWearing(shooter, ModItems.CROWN_OF_IRON)) {
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_TICKS), shooter);
			}
		});
	}
}

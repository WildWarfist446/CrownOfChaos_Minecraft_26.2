package com.crownofchaos.mixin;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Makes a Crown of Wither wearer immune to the Wither effect, the same way Wither Skeletons
 * are: the effect is simply refused, whatever its source (Wither Skeletons, the Wither's
 * skulls, wither roses, potions).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {
	@Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$witherCrownImmunity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		if (effect.is(MobEffects.WITHER) && CrownEffects.isWearingOrChaos((LivingEntity) (Object) this, ModItems.CROWN_OF_WITHER)) {
			cir.setReturnValue(false);
		}
	}
}

package com.crownofchaos.mixin;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;

/**
 * Crown of Winter wearers walk on top of powder snow, like wearing leather boots.
 */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
	private static void crownofchaos$winterCrownWalksOnSnow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof LivingEntity living && CrownEffects.isWearingOrChaos(living, ModItems.CROWN_OF_WINTER)) {
			cir.setReturnValue(true);
		}
	}
}

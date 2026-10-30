package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A Crown of Nether wearer moves through lava the way they move through water: the same drag, speed and
 * sinking, instead of vanilla's slow, heavy wade. (The crown's lava immunity and lava vision come from
 * elsewhere.)
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLavaMixin {
	@Shadow
	protected abstract void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY);

	@Shadow
	protected abstract double getEffectiveGravity();

	@Inject(method = "travelInFluid", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$moveThroughLavaLikeWater(Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.isInWater() && self.isInLava() && CrownEffects.isWearingOrChaos(self, ModItems.CROWN_OF_NETHER)) {
			this.travelInWater(input, this.getEffectiveGravity(), self.getDeltaMovement().y <= 0.0, self.getY());
			ci.cancel();
		}
	}
}

package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Guardian;

/**
 * Guardian units treat air as water, so they keep swimming (and fighting) out of the sea instead
 * of flopping helplessly on the ground. Everything about how a Guardian moves, sounds and breathes
 * asks {@code isInWater}. Their path finding is swapped separately (see {@code UnitManager}).
 */
@Mixin(Entity.class)
public abstract class UnitWaterMixin {
	@Inject(method = "isInWater()Z", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$guardianUnitsSwimInAir(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Guardian && ((Entity) (Object) this).hasAttached(UnitManager.UNIT)) {
			cir.setReturnValue(true);
		}
	}
}

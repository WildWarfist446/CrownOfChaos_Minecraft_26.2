package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.Mob;

/** Army units don't burn in daylight (Strays and Parched otherwise would); wild mobs still do. */
@Mixin(Mob.class)
public abstract class MobSunMixin {
	@Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$unitsDontBurn(CallbackInfoReturnable<Boolean> cir) {
		if (((Mob) (Object) this).hasAttached(UnitManager.UNIT)) {
			cir.setReturnValue(false);
		}
	}
}

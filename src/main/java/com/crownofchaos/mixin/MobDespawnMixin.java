package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.Mob;

/** On Peaceful vanilla removes hostile mobs; army units stay (they never despawn anyway, see MobSunMixin). */
@Mixin(Mob.class)
public abstract class MobDespawnMixin {
	@Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$unitsStay(CallbackInfo ci) {
		if (((Mob) (Object) this).hasAttached(UnitManager.UNIT)) {
			ci.cancel();
		}
	}
}

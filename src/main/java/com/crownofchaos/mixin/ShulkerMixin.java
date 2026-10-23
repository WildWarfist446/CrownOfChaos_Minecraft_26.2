package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.entity.ChaosKing;
import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.monster.Shulker;

/**
 * A shulker hit by a shulker bullet may teleport and leave a new, wild shulker behind. For army units
 * (and the Chaos King's) that would litter the world with hostile shulkers that outlive a reclaimed unit,
 * so those never do it.
 */
@Mixin(Shulker.class)
public abstract class ShulkerMixin {
	@Inject(method = "hitByShulkerBullet", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$noWildCopies(CallbackInfo ci) {
		Shulker self = (Shulker) (Object) this;
		if (self.hasAttached(UnitManager.UNIT) || self.entityTags().contains(ChaosKing.MINION_TAG)) {
			ci.cancel();
		}
	}
}

package com.crownofchaos.mixin;

import com.crownofchaos.ability.ArrowVolleyAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Lets every arrow in a Crown of Iron volley land: targets normally ignore further damage for
 * half a second after being hit, which would block most of the volley.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void crownofchaos$volleyIgnoresInvulnerability(EntityHitResult hitResult, CallbackInfo ci) {
		if (((Entity) (Object) this).entityTags().contains(ArrowVolleyAbility.ARROW_TAG)
				&& hitResult.getEntity() instanceof LivingEntity target) {
			target.invulnerableTime = 0;
		}
	}
}

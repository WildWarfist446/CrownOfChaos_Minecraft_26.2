package com.crownofchaos.mixin;

import com.crownofchaos.ability.ArrowVolleyAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.BlocksAttacks;

/**
 * A raised shield blocks every Crown of Iron volley arrow. Shields normally only block hits
 * from the front, but volley arrows fall from overhead all around the target, so they're
 * treated as coming from straight ahead.
 */
@Mixin(LivingEntity.class)
public abstract class ShieldBlockingMixin {
	@ModifyArg(
			method = "applyItemBlocking",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"),
			index = 2)
	private double crownofchaos$volleyArrowsAlwaysBlockable(DamageSource source, float damage, double angle) {
		if (source.getDirectEntity() != null && source.getDirectEntity().entityTags().contains(ArrowVolleyAbility.ARROW_TAG)) {
			return 0.0;
		}
		return angle;
	}
}

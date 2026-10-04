package com.crownofchaos.mixin;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.LivingEntity;

/**
 * Crown of Winter wearers keep normal grip on ice instead of sliding. Movement is simulated
 * on the client for the local player, so this runs on both sides.
 */
@Mixin(LivingEntity.class)
public abstract class WinterCrownMovementMixin {
	/** Friction of ordinary blocks; ice is far higher (0.98, blue ice 0.989). */
	private static final float NORMAL_FRICTION = 0.6F;

	@ModifyExpressionValue(
			method = "travelInAir",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
	private float crownofchaos$winterCrownIceGrip(float friction) {
		if (friction > NORMAL_FRICTION && CrownEffects.isWearing((LivingEntity) (Object) this, ModItems.CROWN_OF_WINTER)) {
			return NORMAL_FRICTION;
		}
		return friction;
	}
}

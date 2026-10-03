package com.crownofchaos.mixin;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Boosts damage dealt by a Crown of Poseidon wearer while they are in water. Uses the
 * damage source's owning entity, so arrows and tridents they fire count too.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float crownofchaos$poseidonCrownWaterDamage(float damage, @Local(argsOnly = true) DamageSource source) {
		if (source.getEntity() instanceof LivingEntity attacker
				&& attacker.isInWater()
				&& CrownEffects.isWearing(attacker, ModItems.CROWN_OF_POSEIDON)) {
			return damage * CrownEffects.POSEIDON_CROWN_WATER_DAMAGE_MULTIPLIER;
		}

		return damage;
	}
}

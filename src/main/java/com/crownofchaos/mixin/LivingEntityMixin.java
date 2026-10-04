package com.crownofchaos.mixin;

import com.crownofchaos.ability.ArrowVolleyAbility;
import com.crownofchaos.ability.FireChargeAbility;
import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Crown damage bonuses: a Crown of Poseidon wearer deals more damage while in water (arrows
 * and tridents they fire count too), a Crown of Iron wearer's bow and crossbow arrows hit
 * harder, and Crown of Nether fireballs hit harder than a Ghast's.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float crownofchaos$crownDamageBonuses(float damage, @Local(argsOnly = true) DamageSource source) {
		if (source.getEntity() instanceof LivingEntity attacker
				&& attacker.isInWater()
				&& CrownEffects.isWearing(attacker, ModItems.CROWN_OF_POSEIDON)) {
			damage *= CrownEffects.POSEIDON_CROWN_WATER_DAMAGE_MULTIPLIER;
		}

		// Crown of Iron: arrows the wearer fires from a bow or crossbow (not volley arrows).
		if (source.is(DamageTypes.ARROW)
				&& source.getDirectEntity() != null
				&& !source.getDirectEntity().entityTags().contains(ArrowVolleyAbility.ARROW_TAG)
				&& source.getEntity() instanceof LivingEntity shooter
				&& CrownEffects.isWearing(shooter, ModItems.CROWN_OF_IRON)) {
			damage *= CrownEffects.IRON_CROWN_ARROW_DAMAGE_MULTIPLIER;
		}

		// Crown of Nether fireballs: both the direct hit and the explosion come from the fireball.
		if (FireChargeAbility.isCrownFireball(source.getDirectEntity())) {
			damage *= FireChargeAbility.DAMAGE_MULTIPLIER;
		}

		return damage;
	}
}

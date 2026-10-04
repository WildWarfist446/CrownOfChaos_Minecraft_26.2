package com.crownofchaos.mixin;

import com.crownofchaos.ability.WitherSkullAbility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Level;

/**
 * Wither skulls shot by the Crown of Wither explode like the Wither's, hurting entities, but
 * never break blocks.
 */
@Mixin(WitherSkull.class)
public abstract class WitherSkullMixin {
	@WrapOperation(
			method = "onHit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
	private void crownofchaos$noBlockDamage(Level level, Entity source, double x, double y, double z, float radius,
			boolean fire, Level.ExplosionInteraction interaction, Operation<Void> original) {
		if (((Entity) (Object) this).entityTags().contains(WitherSkullAbility.SKULL_TAG)) {
			original.call(level, source, x, y, z, radius, fire, Level.ExplosionInteraction.NONE);
		} else {
			original.call(level, source, x, y, z, radius, fire, interaction);
		}
	}
}

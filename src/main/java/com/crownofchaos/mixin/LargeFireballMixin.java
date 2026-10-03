package com.crownofchaos.mixin;

import com.crownofchaos.ability.FireChargeAbility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;

/**
 * Fireballs shot by the Crown of Nether explode like a Ghast's, hurting and knocking back
 * entities, but never break blocks or start fires.
 */
@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin {
	@WrapOperation(
			method = "onHit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
	private void crownofchaos$noBlockDamage(Level level, Entity source, double x, double y, double z, float radius,
			boolean fire, Level.ExplosionInteraction interaction, Operation<Void> original) {
		if (((Entity) (Object) this).entityTags().contains(FireChargeAbility.FIREBALL_TAG)) {
			original.call(level, source, x, y, z, radius, false, Level.ExplosionInteraction.NONE);
		} else {
			original.call(level, source, x, y, z, radius, fire, interaction);
		}
	}
}

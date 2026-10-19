package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.ability.CrownAbilities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Projectiles made by crown abilities pass through each other when they have the same owner.
 * Fireballs and wither skulls can be hit by other projectiles, so the Crown of Chaos's "The pain
 * of death", which launches a skull and a fireball from the same spot at once, had them knock each
 * other out before they reached anything.
 */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$ownCrownProjectilesPassThrough(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		Projectile self = (Projectile) (Object) this;
		if (entity instanceof Projectile other
				&& self.getOwner() != null && self.getOwner() == other.getOwner()
				&& self.entityTags().contains(CrownAbilities.CROWN_ABILITY_TAG)
				&& other.entityTags().contains(CrownAbilities.CROWN_ABILITY_TAG)) {
			cir.setReturnValue(false);
		}
	}
}

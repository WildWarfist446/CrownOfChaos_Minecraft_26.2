package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.entity.ChaosKing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** The Chaos King's legion drops none of its usual items or gear, only experience: the fight gives no loot to farm. */
@Mixin(LivingEntity.class)
public abstract class LegionDropsMixin {
	@Shadow
	protected abstract void dropExperience(ServerLevel level, Entity killer);

	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$noLegionLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
		if (((Entity) (Object) this).entityTags().contains(ChaosKing.MINION_TAG)) {
			this.dropExperience(level, source.getEntity());
			ci.cancel();
		}
	}
}

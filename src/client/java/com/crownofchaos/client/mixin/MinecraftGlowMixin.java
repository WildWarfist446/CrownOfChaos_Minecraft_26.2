package com.crownofchaos.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.client.UnitClient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Selected army units glow for the player who selected them (only they see it). */
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$glowSelectedUnits(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (UnitClient.isSelected(entity)) {
			cir.setReturnValue(true);
		}
	}
}

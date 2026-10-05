package com.crownofchaos.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.client.UnitClient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Army units glow (an outline through walls, in their faction's color, see {@link UnitOutlineMixin})
 * for the player seeing them; selected ones glow white. Only that player sees it.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$glowSelectedUnits(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (UnitClient.isSelected(entity) || UnitClient.showsOutline(entity)) {
			cir.setReturnValue(true);
		}
	}
}

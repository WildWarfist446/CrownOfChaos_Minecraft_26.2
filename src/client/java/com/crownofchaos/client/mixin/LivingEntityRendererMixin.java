package com.crownofchaos.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.client.UnitColors;
import com.crownofchaos.unit.UnitData;
import com.crownofchaos.unit.UnitManager;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tints army units in their faction's color: the mob's own texture is shaded in a vivid, high-contrast
 * version of the faction's dye color, so every player can tell whose units they are at a glance. Layers drawn
 * on top (armor, glowing eyes, a stray's outer clothes) keep their usual colors.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/** The tint for this render, or null for mobs that aren't units. */
	private static final RenderStateDataKey<Integer> UNIT_TINT = RenderStateDataKey.create(() -> "crownofchaos:unit_tint");

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void crownofchaos$rememberUnitTint(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		UnitData unit = entity.getAttached(UnitManager.UNIT);
		// Render states are reused between mobs, so always set it, clearing it for non-units.
		state.setData(UNIT_TINT, unit == null ? null : ARGB.opaque(UnitColors.vivid(unit.color())));
	}

	@Inject(method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I", at = @At("RETURN"), cancellable = true)
	private void crownofchaos$applyUnitTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
		Integer tint = state.getData(UNIT_TINT);
		if (tint != null) {
			cir.setReturnValue(ARGB.multiply(cir.getReturnValueI(), tint));
		}
	}
}

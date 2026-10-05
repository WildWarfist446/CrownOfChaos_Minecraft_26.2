package com.crownofchaos.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.client.UnitClient;
import com.crownofchaos.client.UnitColors;
import com.crownofchaos.unit.UnitData;
import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.Entity;

/**
 * Draws the outline of an army unit (the glow that shows through walls) in its faction's vivid
 * color. Selected units keep a white outline, so you can still tell them apart (see
 * {@link MinecraftGlowMixin}).
 */
@Mixin(Entity.class)
public abstract class UnitOutlineMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$unitOutlineColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		UnitData unit = self.getAttached(UnitManager.UNIT);
		if (unit != null && self.level().isClientSide()) {
			cir.setReturnValue(UnitClient.isSelected(self) ? 0xFFFFFF : UnitColors.vivid(unit.color()));
		}
	}
}

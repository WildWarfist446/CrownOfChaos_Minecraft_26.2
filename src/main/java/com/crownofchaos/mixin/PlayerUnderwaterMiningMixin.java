package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Crown of Poseidon wearer mines at full speed in water. Vanilla divides the speed by 5 with the eyes in
 * water (unless Aqua Affinity) and by 5 again when not on the ground, which is always true when swimming;
 * both are undone here, so only the crown's wearer in water is affected.
 */
@Mixin(Player.class)
public abstract class PlayerUnderwaterMiningMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void crownofchaos$mineNormallyInWater(BlockState state, CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		if (!self.isInWater() || !CrownEffects.isWearing(self, ModItems.CROWN_OF_POSEIDON)) {
			return;
		}
		float speed = cir.getReturnValueF();
		if (self.isEyeInFluid(FluidTags.WATER)) {
			float submerged = (float) self.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
			if (submerged > 0.0F) {
				speed /= submerged;
			}
		}
		if (!self.onGround()) {
			speed *= 5.0F;
		}
		cir.setReturnValue(speed);
	}
}

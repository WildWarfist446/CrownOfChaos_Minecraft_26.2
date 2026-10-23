package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.dimension.ChaosArena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Fluids do not flow in the Chaos Arena, so its wall of lava stays a wall: it does not pour onto the
 * arena floor or off into the void.
 */
@Mixin(FlowingFluid.class)
public abstract class FluidSpreadMixin {
	@Inject(method = "spread", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$noFlowInTheArena(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfo ci) {
		if (level.dimension() == ChaosArena.DIMENSION) {
			ci.cancel();
		}
	}
}

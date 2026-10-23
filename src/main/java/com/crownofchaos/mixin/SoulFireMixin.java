package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.dimension.ChaosArena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Soul fire only survives on soul sand and soul soil, so the soul fire that rises on the Chaos Arena's
 * altar after the boss is beaten would vanish at once on its netherrack. It is allowed to stay there,
 * and nowhere else: soul fire anywhere else, and fire on netherrack anywhere else, behave as usual.
 */
@Mixin(SoulFireBlock.class)
public abstract class SoulFireMixin {
	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$altarSoulFire(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (level instanceof Level world && world.dimension() == ChaosArena.DIMENSION && pos.equals(ChaosArena.ALTAR.above())) {
			cir.setReturnValue(true);
		}
	}
}

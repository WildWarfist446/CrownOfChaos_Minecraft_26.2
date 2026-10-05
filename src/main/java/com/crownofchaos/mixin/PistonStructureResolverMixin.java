package com.crownofchaos.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.faction.FactionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

/**
 * A piston can't move if it would destroy a faction flag (pistons break banners in their way)
 * or push or pull away the block a flag stands on or hangs from.
 */
@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {
	@Shadow
	@Final
	private Level level;

	@Shadow
	@Final
	private List<BlockPos> toPush;

	@Shadow
	@Final
	private List<BlockPos> toDestroy;

	@Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
	private void crownofchaos$protectFactionFlags(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && this.level instanceof ServerLevel serverLevel
				&& (this.toPush.stream().anyMatch(pos -> FactionManager.isFlagProtected(serverLevel, pos))
						|| this.toDestroy.stream().anyMatch(pos -> FactionManager.isFlagProtected(serverLevel, pos)))) {
			cir.setReturnValue(false);
		}
	}
}

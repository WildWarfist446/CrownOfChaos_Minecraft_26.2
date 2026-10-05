package com.crownofchaos.mixin;

import java.util.ArrayList;
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
import net.minecraft.world.level.ServerExplosion;

/**
 * Explosions (creepers, TNT, beds, Withers and so on) don't destroy faction flags or the blocks
 * holding them up; everything else around them breaks as normal.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
	private void crownofchaos$spareFactionFlags(CallbackInfoReturnable<List<BlockPos>> cir) {
		List<BlockPos> positions = cir.getReturnValue();
		if (positions.stream().anyMatch(pos -> FactionManager.isFlagProtected(this.level, pos))) {
			List<BlockPos> spared = new ArrayList<>(positions);
			spared.removeIf(pos -> FactionManager.isFlagProtected(this.level, pos));
			cir.setReturnValue(spared);
		}
	}
}

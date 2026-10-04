package com.crownofchaos.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;

/**
 * Every Ender Dragon kill places a Dragon Egg on the exit portal, not just the first, so a
 * resummoned dragon gives another egg (needed to craft the Crown of Void). It's skipped if an
 * egg from an earlier fight is still sitting there.
 */
@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {
	@Shadow
	private ServerLevel level;

	@Shadow
	private BlockPos origin;

	@ModifyExpressionValue(
			method = "setDragonKilled",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;hasPreviouslyKilledDragon:Z"))
	private boolean crownofchaos$eggOnEveryKill(boolean previouslyKilled) {
		BlockPos top = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(this.origin));
		boolean eggAlreadyThere = this.level.getBlockState(top.below()).is(Blocks.DRAGON_EGG);
		// Vanilla places the egg when this is false.
		return eggAlreadyThere;
	}
}

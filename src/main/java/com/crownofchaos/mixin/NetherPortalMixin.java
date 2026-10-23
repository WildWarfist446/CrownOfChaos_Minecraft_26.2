package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.portal.GlitchedPortals;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Disc 11 thrown into a nether portal is swallowed and turns the portal into a glitched one
 * (see {@link GlitchedPortals}).
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$discGlitchesPortal(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise, CallbackInfo ci) {
		if (level instanceof ServerLevel serverLevel && entity instanceof ItemEntity item && item.getItem().is(Items.MUSIC_DISC_11)
				&& GlitchedPortals.convert(serverLevel, pos)) {
			item.getItem().shrink(1);
			if (item.getItem().isEmpty()) {
				item.discard();
			}
			ci.cancel();
		}
	}
}

package com.crownofchaos.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.world.attribute.EnvironmentAttributes;

/**
 * A Crown of Nether wearer sees through lava as clearly as through water. Vanilla fogs lava in at a
 * fraction of a block (5 blocks even with Fire Resistance); with the crown the fog starts and ends where
 * water's does in the same place, so the fog still tints everything orange but you can see far.
 */
@Mixin(LavaFogEnvironment.class)
public abstract class LavaFogMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void crownofchaos$seeThroughLava(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (camera.entity() instanceof LocalPlayer player && !player.isSpectator() && CrownEffects.isWearingOrChaos(player, ModItems.CROWN_OF_NETHER)) {
			float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
			fog.environmentalStart = camera.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_START_DISTANCE, partialTicks);
			fog.environmentalEnd = camera.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_END_DISTANCE, partialTicks);
			fog.skyEnd = fog.environmentalEnd;
			fog.cloudEnd = fog.environmentalEnd;
		}
	}
}

package com.crownofchaos.client;

import com.crownofchaos.client.faction.FactionClientState;
import com.crownofchaos.client.model.CrownModels;
import com.crownofchaos.client.render.IcicleRenderer;
import com.crownofchaos.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CrownOfChaosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CrownAbilityKeybind.initialize();
		CrownAbilityClientHandlers.initialize();
		OreSenseClient.initialize();
		FactionKeybind.initialize();
		FactionClientState.initialize();
		CrownModels.initialize();
		EntityRendererRegistry.register(ModEntities.ICICLE, IcicleRenderer::new);
	}
}

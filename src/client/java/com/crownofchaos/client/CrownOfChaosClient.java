package com.crownofchaos.client;

import com.crownofchaos.client.model.CrownModels;

import net.fabricmc.api.ClientModInitializer;

public class CrownOfChaosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CrownAbilityKeybind.initialize();
		CrownAbilityClientHandlers.initialize();
		CrownModels.initialize();
	}
}

package com.crownofchaos.client;

import net.fabricmc.api.ClientModInitializer;

public class CrownOfChaosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CrownAbilityKeybind.initialize();
		CrownAbilityClientHandlers.initialize();
	}
}

package com.crownofchaos.client;

import com.crownofchaos.client.faction.FactionClientState;
import com.crownofchaos.client.menu.CommanderTableScreen;
import com.crownofchaos.client.model.CrownModels;
import com.crownofchaos.client.render.IcicleRenderer;
import com.crownofchaos.entity.ModEntities;
import com.crownofchaos.menu.ModMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.gui.screens.MenuScreens;

public class CrownOfChaosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CrownAbilityKeybind.initialize();
		CrownAbilityClientHandlers.initialize();
		OreSenseClient.initialize();
		FactionKeybind.initialize();
		FactionClientState.initialize();
		UnitClient.initialize();
		BossMusicClient.initialize();
		VoidElytraClient.initialize();
		CrownModels.initialize();
		EntityRendererRegistry.register(ModEntities.ICICLE, IcicleRenderer::new);
		EntityRendererRegistry.register(ModEntities.CHAOS_KING, com.crownofchaos.client.render.ChaosKingRenderer::new);
		MenuScreens.register(ModMenus.COMMANDER_TABLE, CommanderTableScreen::new);
	}
}

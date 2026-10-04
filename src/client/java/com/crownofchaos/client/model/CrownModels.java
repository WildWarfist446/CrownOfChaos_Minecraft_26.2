package com.crownofchaos.client.model;

import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/**
 * Registers the crowns' custom 3D models. Crowns without one still render as a diamond
 * helmet via their equipment asset.
 */
public final class CrownModels {
	private CrownModels() {
	}

	public static void initialize() {
		ModelLayerRegistry.registerModelLayer(UnderCrownModel.LAYER, UnderCrownModel::createLayer);
		ArmorRenderer.register(
				context -> new CrownArmorRenderer(context.bakeLayer(UnderCrownModel.LAYER), UnderCrownModel.TEXTURE),
				ModItems.UNDER_CROWN);

		ModelLayerRegistry.registerModelLayer(CrownOfPoseidonModel.LAYER, CrownOfPoseidonModel::createLayer);
		ArmorRenderer.register(
				context -> new CrownArmorRenderer(context.bakeLayer(CrownOfPoseidonModel.LAYER), CrownOfPoseidonModel.TEXTURE),
				ModItems.CROWN_OF_POSEIDON);
	}
}

package com.crownofchaos.client.model;

import com.crownofchaos.CrownOfChaos;
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
				ModItems.UNDER_CROWN, ModItems.NETHERITE_UNDER_CROWN);

		ModelLayerRegistry.registerModelLayer(CrownOfPoseidonModel.LAYER, CrownOfPoseidonModel::createLayer);
		ArmorRenderer.register(
				context -> new CrownArmorRenderer(context.bakeLayer(CrownOfPoseidonModel.LAYER), CrownOfPoseidonModel.TEXTURE),
				ModItems.CROWN_OF_POSEIDON, ModItems.NETHERITE_CROWN_OF_POSEIDON);

		ModelLayerRegistry.registerModelLayer(CrownOfNetherModel.LAYER, CrownOfNetherModel::createLayer);
		ArmorRenderer.register(
				context -> new CrownArmorRenderer(context.bakeLayer(CrownOfNetherModel.LAYER), CrownOfNetherModel.TEXTURE),
				ModItems.CROWN_OF_NETHER, ModItems.NETHERITE_CROWN_OF_NETHER);

		ModelLayerRegistry.registerModelLayer(CrownOfWitherModel.LAYER, CrownOfWitherModel::createLayer);
		ArmorRenderer.register(
				context -> new CrownArmorRenderer(context.bakeLayer(CrownOfWitherModel.LAYER), CrownOfWitherModel.TEXTURE),
				ModItems.CROWN_OF_WITHER, ModItems.NETHERITE_CROWN_OF_WITHER);

		// The Crown of Darkness uses an item model JSON rather than an entity model.
		ArmorRenderer.register(
				context -> new CrownItemModelArmorRenderer(CrownOfChaos.id("crown_of_darkness_worn")),
				ModItems.CROWN_OF_DARKNESS, ModItems.NETHERITE_CROWN_OF_DARKNESS);
	}
}

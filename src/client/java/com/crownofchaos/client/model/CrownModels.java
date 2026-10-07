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

		// The Golden Crown and the Crowns of Darkness, Sun, Iron and Void use item model JSONs rather than entity models.
		ArmorRenderer.register(
				context -> new CrownItemModelArmorRenderer(CrownOfChaos.id("golden_crown_worn")),
				ModItems.GOLDEN_CROWN);
		ArmorRenderer.register(
				context -> new CrownItemModelArmorRenderer(CrownOfChaos.id("crown_of_darkness_worn")),
				ModItems.CROWN_OF_DARKNESS, ModItems.NETHERITE_CROWN_OF_DARKNESS);
		ArmorRenderer.register(
				context -> new CrownItemModelArmorRenderer(CrownOfChaos.id("crown_of_sun_worn")),
				ModItems.CROWN_OF_SUN, ModItems.NETHERITE_CROWN_OF_SUN);
		ArmorRenderer.register(
				context -> new CrownItemModelArmorRenderer(CrownOfChaos.id("crown_of_iron_worn")),
				ModItems.CROWN_OF_IRON, ModItems.NETHERITE_CROWN_OF_IRON);
		ArmorRenderer.register(
				context -> new CrownItemModelArmorRenderer(CrownOfChaos.id("crown_of_void_worn")),
				ModItems.CROWN_OF_VOID, ModItems.NETHERITE_CROWN_OF_VOID);
	}
}

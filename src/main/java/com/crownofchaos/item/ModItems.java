package com.crownofchaos.item;

import java.util.function.Function;

import com.crownofchaos.CrownOfChaos;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModItems {
	// Same armor, toughness, durability and repair item as a diamond helmet, and it renders
	// as a diamond helmet when worn until the custom 3D model is ready.
	public static final Item UNDER_CROWN = register("under_crown", Item::new, new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CrownOfChaos.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> output.insertAfter(Items.NETHERITE_BOOTS, UNDER_CROWN));
	}
}

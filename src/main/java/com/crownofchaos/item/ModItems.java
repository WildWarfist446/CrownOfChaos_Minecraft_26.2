package com.crownofchaos.item;

import java.util.List;
import java.util.function.Function;

import com.crownofchaos.CrownOfChaos;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public final class ModItems {
	// Crowns have the same armor, toughness, durability and repair item as a diamond helmet, and they render
	// as a diamond helmet when worn until the custom 3D model is ready.
	public static final Item UNDER_CROWN = register("under_crown", Item::new, new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC));

	public static final Item CROWN_OF_POSEIDON = register("crown_of_poseidon", Item::new, new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC));

	public static final Item CROWN_OF_NETHER = register("crown_of_nether", Item::new, new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC));

	public static final Item CROWN_OF_DARKNESS = register("crown_of_darkness", Item::new, new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC));

	/** Dropped by Wardens; used to craft the Crown of Darkness. */
	public static final Item SCULK_CORD = register("sculk_cord", Item::new, new Item.Properties()
			.rarity(Rarity.RARE)
			.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.crownofchaos.sculk_cord.lore")))));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CrownOfChaos.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> output.insertAfter(Items.NETHERITE_BOOTS, UNDER_CROWN, CROWN_OF_POSEIDON, CROWN_OF_NETHER, CROWN_OF_DARKNESS));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.insertAfter(Items.ECHO_SHARD, SCULK_CORD));

		// Wardens always drop one Sculk Cord, alongside their usual Sculk Catalyst.
		EntityTypes.WARDEN.getDefaultLootTable().ifPresent(wardenLoot ->
				LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
					if (key.equals(wardenLoot) && source.isBuiltin()) {
						tableBuilder.withPool(LootPool.lootPool()
								.setRolls(ConstantValue.exactly(1.0F))
								.add(LootItem.lootTableItem(SCULK_CORD)));
					}
				}));
	}
}

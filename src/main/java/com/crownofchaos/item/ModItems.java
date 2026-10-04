package com.crownofchaos.item;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.crownofchaos.CrownOfChaos;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

public final class ModItems {
	/** The base crown every other crown is crafted from. Golden helmet stats. */
	public static final Item GOLDEN_CROWN = register("golden_crown", Item::new, withLore("golden_crown", new Item.Properties()
			.humanoidArmor(ArmorMaterials.GOLD, ArmorType.HELMET)
			.rarity(Rarity.UNCOMMON)));

	// Crowns have the same armor, toughness, durability and repair item as a diamond helmet, and they render
	// as a diamond helmet when worn until the custom 3D model is ready.
	public static final Item UNDER_CROWN = register("under_crown", Item::new, withLore("under_crown", new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC)));

	public static final Item CROWN_OF_POSEIDON = register("crown_of_poseidon", Item::new, withLore("crown_of_poseidon", new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC)));

	public static final Item CROWN_OF_NETHER = register("crown_of_nether", Item::new, withLore("crown_of_nether", new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC)));

	public static final Item CROWN_OF_DARKNESS = register("crown_of_darkness", Item::new, withLore("crown_of_darkness", new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC)));

	public static final Item CROWN_OF_SUN = register("crown_of_sun", Item::new, withLore("crown_of_sun", new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)
			.rarity(Rarity.EPIC)));

	/** Dropped by Wardens; used to craft the Crown of Darkness. */
	public static final Item SCULK_CORD = register("sculk_cord", Item::new, withLore("sculk_cord", new Item.Properties()
			.rarity(Rarity.RARE)));

	/** Found in desert pyramid chests; used to craft the Crown of Sun. */
	public static final Item ANKH_CHARM = register("ankh_charm", Item::new, withLore("ankh_charm", new Item.Properties()
			.rarity(Rarity.RARE)));

	private ModItems() {
	}

	/** Adds the italic description line {@code item.crownofchaos.<name>.lore} from the language file. */
	private static Item.Properties withLore(String name, Item.Properties properties) {
		return properties.component(DataComponents.LORE,
				new ItemLore(List.of(Component.translatable("item.crownofchaos." + name + ".lore"))));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CrownOfChaos.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static boolean isAnkhCharmChest(LootContext context) {
		Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
		if (origin == null) {
			return false;
		}

		ServerLevel level = context.getLevel();
		BlockPos chestPos = BlockPos.containing(origin);
		StructureStart pyramid = level.structureManager()
				.getStructureWithPieceAt(chestPos, structure -> structure.is(BuiltinStructures.DESERT_PYRAMID));
		if (!pyramid.isValid()) {
			return false;
		}

		// The four chests sit 2 blocks east, west, south and north of the pyramid's center.
		BlockPos center = pyramid.getBoundingBox().getCenter();
		int dx = chestPos.getX() - center.getX();
		int dz = chestPos.getZ() - center.getZ();
		int chest = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? 0 : 1) : (dz >= 0 ? 2 : 3);

		ChunkPos pyramidChunk = pyramid.getChunkPos();
		int chosen = Math.floorMod(Objects.hash(pyramidChunk.x(), pyramidChunk.z(), level.getSeed()), 4);
		return chest == chosen;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> output.insertAfter(Items.NETHERITE_BOOTS, GOLDEN_CROWN, UNDER_CROWN, CROWN_OF_POSEIDON, CROWN_OF_NETHER, CROWN_OF_DARKNESS, CROWN_OF_SUN));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.insertAfter(Items.ECHO_SHARD, SCULK_CORD, ANKH_CHARM));

		ResourceKey<LootTable> wardenLoot = EntityTypes.WARDEN.getDefaultLootTable().orElseThrow();
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}

			// Wardens always drop one Sculk Cord, alongside their usual Sculk Catalyst.
			if (key.equals(wardenLoot)) {
				tableBuilder.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(SCULK_CORD)));
			}

		});

		// Every desert pyramid has exactly one Ankh Charm: each pyramid picks one of its four
		// chests, the same one whichever order they're opened in.
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (table.is(BuiltInLootTables.DESERT_PYRAMID) && isAnkhCharmChest(context)) {
				drops.add(new ItemStack(ANKH_CHARM));
			}
		});
	}
}

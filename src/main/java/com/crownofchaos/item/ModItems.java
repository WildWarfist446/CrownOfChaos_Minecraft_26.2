package com.crownofchaos.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.block.ModBlocks;
import com.crownofchaos.unit.ModUnits;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
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
	/** Every crown and netherite crown. Declared first, since the crowns below add themselves as they're registered. */
	private static final Set<Item> CROWNS = new HashSet<>();

	/** The base crown every other crown is crafted from. Golden helmet stats. */
	public static final Item GOLDEN_CROWN = registerCrown("golden_crown", ArmorMaterials.GOLD, Rarity.UNCOMMON, false);

	// The magical crowns have the same armor, toughness, durability and repair item as a diamond
	// helmet, and render as a diamond helmet when worn until their custom 3D model is ready.
	public static final Item UNDER_CROWN = registerCrown("under_crown", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_POSEIDON = registerCrown("crown_of_poseidon", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_NETHER = registerCrown("crown_of_nether", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_DARKNESS = registerCrown("crown_of_darkness", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_SUN = registerCrown("crown_of_sun", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_WITHER = registerCrown("crown_of_wither", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_WINTER = registerCrown("crown_of_winter", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_IRON = registerCrown("crown_of_iron", ArmorMaterials.DIAMOND, Rarity.EPIC);
	public static final Item CROWN_OF_VOID = registerCrown("crown_of_void", ArmorMaterials.DIAMOND, Rarity.EPIC);

	/** Netherite versions, made from the crowns above in a smithing table. Maps each to its crown. */
	private static final Map<Item, Item> NETHERITE_TO_BASE = new HashMap<>();

	// Netherite helmet stats and they don't burn; otherwise identical to the crown they came from,
	// including its effects, abilities, sprite and 3D model.
	public static final Item NETHERITE_UNDER_CROWN = registerNetheriteCrown("under_crown", UNDER_CROWN);
	public static final Item NETHERITE_CROWN_OF_POSEIDON = registerNetheriteCrown("crown_of_poseidon", CROWN_OF_POSEIDON);
	public static final Item NETHERITE_CROWN_OF_NETHER = registerNetheriteCrown("crown_of_nether", CROWN_OF_NETHER);
	public static final Item NETHERITE_CROWN_OF_DARKNESS = registerNetheriteCrown("crown_of_darkness", CROWN_OF_DARKNESS);
	public static final Item NETHERITE_CROWN_OF_SUN = registerNetheriteCrown("crown_of_sun", CROWN_OF_SUN);
	public static final Item NETHERITE_CROWN_OF_WITHER = registerNetheriteCrown("crown_of_wither", CROWN_OF_WITHER);
	public static final Item NETHERITE_CROWN_OF_WINTER = registerNetheriteCrown("crown_of_winter", CROWN_OF_WINTER);
	public static final Item NETHERITE_CROWN_OF_IRON = registerNetheriteCrown("crown_of_iron", CROWN_OF_IRON);
	public static final Item NETHERITE_CROWN_OF_VOID = registerNetheriteCrown("crown_of_void", CROWN_OF_VOID);

	/** Dropped by Wardens; used to craft the Crown of Darkness. */
	public static final Item SCULK_CORD = register("sculk_cord", Item::new, withLore("sculk_cord", new Item.Properties()
			.rarity(Rarity.RARE)));

	/** Found in desert pyramid chests; used to craft the Crown of Sun. */
	public static final Item ANKH_CHARM = register("ankh_charm", Item::new, withLore("ankh_charm", new Item.Properties()
			.rarity(Rarity.RARE)));

	/** Crafted from 8 gold ingots around an emerald; a material for faction recipes. */
	public static final Item FACTION_COIN = register("faction_coin", Item::new, new Item.Properties()
			.rarity(Rarity.UNCOMMON));

	/** Claims land for the crafter's faction when placed (see {@link FactionFlagItem}). */
	public static final Item FACTION_FLAG = register("faction_flag", FactionFlagItem::new, new Item.Properties()
			.stacksTo(16));

	/** The mod's own creative tab, holding every Crown of Chaos item. */
	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, CrownOfChaos.id("crown_of_chaos"));

	private ModItems() {
	}

	/**
	 * Registers a magical crown: a helmet of the given armor material with its description line
	 * and the enchanted glint, which every magical crown has whether or not it's enchanted.
	 */
	private static Item registerCrown(String name, ArmorMaterial material, Rarity rarity) {
		return registerCrown(name, material, rarity, true);
	}

	/**
	 * Registers a crown, with the enchanted glint always on if {@code glint}, or, like normal
	 * gear, only when enchanted (the Golden Crown, which isn't magical).
	 */
	private static Item registerCrown(String name, ArmorMaterial material, Rarity rarity, boolean glint) {
		Item.Properties properties = new Item.Properties()
				.humanoidArmor(material, ArmorType.HELMET)
				.rarity(rarity);
		if (glint) {
			properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		Item crown = register(name, Item::new, withLore(name, properties));
		CROWNS.add(crown);
		return crown;
	}

	/**
	 * Registers the netherite version of a crown: netherite helmet stats, fire-proof like other
	 * netherite gear, and the crown's own description, glint and look.
	 */
	private static Item registerNetheriteCrown(String baseName, Item base) {
		Item netherite = register("netherite_" + baseName, Item::new, new Item.Properties()
				.humanoidArmor(ArmorMaterials.NETHERITE, ArmorType.HELMET)
				.rarity(Rarity.EPIC)
				.fireResistant()
				// Keep the diamond helmet look when worn (crowns with a 3D model draw that instead).
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
						.setEquipSound(ArmorMaterials.NETHERITE.equipSound())
						.setAsset(ArmorMaterials.DIAMOND.assetId())
						.build())
				.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
				.component(DataComponents.LORE, new ItemLore(List.of(Component.translatableWithFallback("item.crownofchaos." + baseName + ".lore", "")))));
		NETHERITE_TO_BASE.put(netherite, base);
		CROWNS.add(netherite);
		return netherite;
	}

	/** Whether the item is one of the mod's crowns, including netherite crowns. */
	public static boolean isCrown(Item item) {
		return CROWNS.contains(item);
	}

	/**
	 * The crown an item counts as for effects and abilities: the original crown for a netherite
	 * crown, otherwise the item itself.
	 */
	public static Item baseCrown(Item item) {
		return NETHERITE_TO_BASE.getOrDefault(item, item);
	}

	/** Adds the italic description line {@code item.crownofchaos.<name>.lore} from the language file. */
	private static Item.Properties withLore(String name, Item.Properties properties) {
		return properties.component(DataComponents.LORE,
				new ItemLore(List.of(Component.translatableWithFallback("item.crownofchaos." + name + ".lore", ""))));
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
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.crownofchaos.crown_of_chaos"))
				.icon(() -> new ItemStack(CROWN_OF_DARKNESS))
				.displayItems((parameters, output) -> {
					output.accept(GOLDEN_CROWN);
					output.accept(UNDER_CROWN);
					output.accept(CROWN_OF_POSEIDON);
					output.accept(CROWN_OF_NETHER);
					output.accept(CROWN_OF_DARKNESS);
					output.accept(CROWN_OF_SUN);
					output.accept(CROWN_OF_WITHER);
					output.accept(CROWN_OF_WINTER);
					output.accept(CROWN_OF_IRON);
					output.accept(CROWN_OF_VOID);
					output.accept(NETHERITE_UNDER_CROWN);
					output.accept(NETHERITE_CROWN_OF_POSEIDON);
					output.accept(NETHERITE_CROWN_OF_NETHER);
					output.accept(NETHERITE_CROWN_OF_DARKNESS);
					output.accept(NETHERITE_CROWN_OF_SUN);
					output.accept(NETHERITE_CROWN_OF_WITHER);
					output.accept(NETHERITE_CROWN_OF_WINTER);
					output.accept(NETHERITE_CROWN_OF_IRON);
					output.accept(NETHERITE_CROWN_OF_VOID);
					output.accept(SCULK_CORD);
					output.accept(ANKH_CHARM);
					output.accept(FACTION_COIN);
					output.accept(FACTION_FLAG);
					output.accept(ModBlocks.COMMANDER_TABLE);
					ModUnits.all().forEach(unit -> output.accept(unit.item()));
				})
				.build());

		// Also listed alongside similar vanilla items.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> output.insertAfter(Items.NETHERITE_BOOTS, GOLDEN_CROWN, UNDER_CROWN, CROWN_OF_POSEIDON, CROWN_OF_NETHER, CROWN_OF_DARKNESS, CROWN_OF_SUN, CROWN_OF_WITHER, CROWN_OF_WINTER, CROWN_OF_IRON, CROWN_OF_VOID,
						NETHERITE_UNDER_CROWN, NETHERITE_CROWN_OF_POSEIDON, NETHERITE_CROWN_OF_NETHER, NETHERITE_CROWN_OF_DARKNESS, NETHERITE_CROWN_OF_SUN, NETHERITE_CROWN_OF_WITHER, NETHERITE_CROWN_OF_WINTER, NETHERITE_CROWN_OF_IRON, NETHERITE_CROWN_OF_VOID));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.insertAfter(Items.ECHO_SHARD, SCULK_CORD, ANKH_CHARM, FACTION_COIN));

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

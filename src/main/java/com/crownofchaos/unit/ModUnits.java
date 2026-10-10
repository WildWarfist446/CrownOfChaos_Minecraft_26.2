package com.crownofchaos.unit;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.item.ModItems;
import com.crownofchaos.item.UnitItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Every army unit, from Warfist's plan, grouped by the crown that commands it. Each one costs
 * two kinds of item at a Commander's Table, set by Warfist (see
 * {@code developing plans/Unit Recipes.md}).
 */
public final class ModUnits {
	private static final List<UnitType> ALL = new ArrayList<>();

	// Crown of Wither
	public static final UnitType WITHER_SKELETON = unit("wither_skeleton", ModItems.CROWN_OF_WITHER, EntityTypes.WITHER_SKELETON, cost(Items.NETHER_WART, 20), cost(Items.IRON_SWORD, 1));
	// Crown of Winter
	public static final UnitType STRAY = unit("stray", ModItems.CROWN_OF_WINTER, EntityTypes.STRAY, cost(Items.SNOW_BLOCK, 15), cost(Items.BOW, 1));
	public static final UnitType POLAR_BEAR = unit("polar_bear", ModItems.CROWN_OF_WINTER, EntityTypes.POLAR_BEAR, UnitType.Cost.anyOf(ItemTags.WOOL, Items.WOOL.white(), 15), cost(Items.COD, 3));
	public static final UnitType SNOW_GOLEM = unit("snow_golem", ModItems.CROWN_OF_WINTER, EntityTypes.SNOW_GOLEM, cost(Items.SNOW_BLOCK, 4), cost(Items.JACK_O_LANTERN, 1));
	// Crown of Iron
	public static final UnitType PILLAGER = unit("pillager", ModItems.CROWN_OF_IRON, EntityTypes.PILLAGER, cost(Items.EMERALD, 3), cost(Items.CROSSBOW, 1));
	public static final UnitType VINDICATOR = unit("vindicator", ModItems.CROWN_OF_IRON, EntityTypes.VINDICATOR, cost(Items.EMERALD, 5), cost(Items.IRON_AXE, 1));
	public static final UnitType RAVAGER = unit("ravager", ModItems.CROWN_OF_IRON, EntityTypes.RAVAGER, cost(Items.EMERALD, 20), cost(Items.SADDLE, 1));
	public static final UnitType EVOKER = unit("evoker", ModItems.CROWN_OF_IRON, EntityTypes.EVOKER, cost(Items.EMERALD, 15), cost(Items.BLAZE_POWDER, 4));
	// Crown of Darkness
	public static final UnitType WARDEN = unit("warden", ModItems.CROWN_OF_DARKNESS, EntityTypes.WARDEN, cost(Items.DIAMOND, 10), cost(Items.ECHO_SHARD, 1));
	// Crown of Sun
	public static final UnitType HUSK = unit("husk", ModItems.CROWN_OF_SUN, EntityTypes.HUSK, cost(Items.ROTTEN_FLESH, 5), cost(Items.GOLD_INGOT, 2));
	public static final UnitType PARCHED = unit("parched", ModItems.CROWN_OF_SUN, EntityTypes.PARCHED, cost(Items.BONE, 10), cost(Items.BOW, 1));
	// Crown of Poseidon ("30 seaweed": Kelp, the long seaweed)
	public static final UnitType GUARDIAN = unit("guardian", ModItems.CROWN_OF_POSEIDON, EntityTypes.GUARDIAN, cost(Items.KELP, 30), cost(Items.PRISMARINE_SHARD, 2));
	/** Always summoned holding a trident, for long-range attacks (see {@link UnitManager#makeUnit}). */
	public static final UnitType DROWNED = unit("drowned", ModItems.CROWN_OF_POSEIDON, EntityTypes.DROWNED, cost(Items.ROTTEN_FLESH, 10), cost(Items.WATER_BUCKET, 1));
	// Crown of Nether
	public static final UnitType PIGLIN = unit("piglin", ModItems.CROWN_OF_NETHER, EntityTypes.PIGLIN, cost(Items.PORKCHOP, 5), cost(Items.GOLD_INGOT, 3));
	public static final UnitType HOGLIN = unit("hoglin", ModItems.CROWN_OF_NETHER, EntityTypes.HOGLIN, cost(Items.PORKCHOP, 10), cost(Items.GOLD_INGOT, 5));
	public static final UnitType GHAST = unit("ghast", ModItems.CROWN_OF_NETHER, EntityTypes.GHAST, cost(Items.BLAZE_POWDER, 10), cost(Items.GHAST_TEAR, 1));
	// Crown of Void
	public static final UnitType ENDERMAN = unit("enderman", ModItems.CROWN_OF_VOID, EntityTypes.ENDERMAN, cost(Items.ENDER_PEARL, 5), cost(Items.END_STONE, 5));
	public static final UnitType SHULKER = unit("shulker", ModItems.CROWN_OF_VOID, EntityTypes.SHULKER, cost(Items.PURPUR_BLOCK, 5), cost(Items.DIAMOND, 2));

	private ModUnits() {
	}

	private static UnitType.Cost cost(Item item, int count) {
		return UnitType.Cost.of(item, count);
	}

	private static UnitType unit(String id, Item crown, EntityType<?> entity, UnitType.Cost costA, UnitType.Cost costB) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CrownOfChaos.id(id + "_unit"));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new UnitItem(new Item.Properties().setId(key)));
		UnitType unit = new UnitType(id, crown, entity, item, costA, costB);
		ALL.add(unit);
		return unit;
	}

	public static List<UnitType> all() {
		return ALL;
	}

	/** The units a crown commands (a netherite crown commands the same as its base crown), in list order. */
	public static List<UnitType> forCrown(Item crown) {
		Item base = ModItems.baseCrown(crown);
		return ALL.stream().filter(unit -> unit.crown() == base).toList();
	}

	public static @Nullable UnitType byId(String id) {
		return ALL.stream().filter(unit -> unit.id().equals(id)).findFirst().orElse(null);
	}

	public static @Nullable UnitType byItem(Item item) {
		return ALL.stream().filter(unit -> unit.item() == item).findFirst().orElse(null);
	}

	/** Loads this class so the unit items are registered. */
	public static void initialize() {
	}
}

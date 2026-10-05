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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Every army unit, from Warfist's plan, grouped by the crown that commands it. Each one costs
 * two items at a Commander's Table.
 *
 * <p>The costs are placeholders (1 Iron Ingot and 1 Gold Ingot) until Warfist sets the real
 * recipes; change them in the list below.
 */
public final class ModUnits {
	private static final UnitType.Cost PLACEHOLDER_A = new UnitType.Cost(Items.IRON_INGOT, 1);
	private static final UnitType.Cost PLACEHOLDER_B = new UnitType.Cost(Items.GOLD_INGOT, 1);

	private static final List<UnitType> ALL = new ArrayList<>();

	// Crown of Wither
	public static final UnitType WITHER_SKELETON = unit("wither_skeleton", ModItems.CROWN_OF_WITHER, EntityTypes.WITHER_SKELETON, PLACEHOLDER_A, PLACEHOLDER_B);
	// Crown of Winter
	public static final UnitType STRAY = unit("stray", ModItems.CROWN_OF_WINTER, EntityTypes.STRAY, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType POLAR_BEAR = unit("polar_bear", ModItems.CROWN_OF_WINTER, EntityTypes.POLAR_BEAR, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType SNOW_GOLEM = unit("snow_golem", ModItems.CROWN_OF_WINTER, EntityTypes.SNOW_GOLEM, PLACEHOLDER_A, PLACEHOLDER_B);
	// Crown of Iron
	public static final UnitType PILLAGER = unit("pillager", ModItems.CROWN_OF_IRON, EntityTypes.PILLAGER, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType VINDICATOR = unit("vindicator", ModItems.CROWN_OF_IRON, EntityTypes.VINDICATOR, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType RAVAGER = unit("ravager", ModItems.CROWN_OF_IRON, EntityTypes.RAVAGER, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType EVOKER = unit("evoker", ModItems.CROWN_OF_IRON, EntityTypes.EVOKER, PLACEHOLDER_A, PLACEHOLDER_B);
	// Crown of Darkness
	public static final UnitType WARDEN = unit("warden", ModItems.CROWN_OF_DARKNESS, EntityTypes.WARDEN, PLACEHOLDER_A, PLACEHOLDER_B);
	// Crown of Sun
	public static final UnitType HUSK = unit("husk", ModItems.CROWN_OF_SUN, EntityTypes.HUSK, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType PARCHED = unit("parched", ModItems.CROWN_OF_SUN, EntityTypes.PARCHED, PLACEHOLDER_A, PLACEHOLDER_B);
	// Crown of Poseidon
	public static final UnitType GUARDIAN = unit("guardian", ModItems.CROWN_OF_POSEIDON, EntityTypes.GUARDIAN, PLACEHOLDER_A, PLACEHOLDER_B);
	// Crown of Nether
	public static final UnitType PIGLIN = unit("piglin", ModItems.CROWN_OF_NETHER, EntityTypes.PIGLIN, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType HOGLIN = unit("hoglin", ModItems.CROWN_OF_NETHER, EntityTypes.HOGLIN, PLACEHOLDER_A, PLACEHOLDER_B);
	public static final UnitType GHAST = unit("ghast", ModItems.CROWN_OF_NETHER, EntityTypes.GHAST, PLACEHOLDER_A, PLACEHOLDER_B);

	private ModUnits() {
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

	public static @Nullable UnitType byItem(Item item) {
		return ALL.stream().filter(unit -> unit.item() == item).findFirst().orElse(null);
	}

	/** Loads this class so the unit items are registered. */
	public static void initialize() {
	}
}

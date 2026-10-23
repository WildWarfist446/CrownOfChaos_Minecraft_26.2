package com.crownofchaos.block;

import java.util.function.Function;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The mod's blocks, each with its block item. */
public final class ModBlocks {
	/**
	 * Makes army units from a crown. Breaks and sounds like wood, and uses Warfist's 3D model.
	 */
	public static final Block COMMANDER_TABLE = register("commander_table", CommanderTableBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(2.5F)
			.sound(SoundType.WOOD)
			.noOcclusion());

	/** The swirl of a glitched portal: drawn with Minecraft's missing-texture purple and black. No item. */
	public static final Block NULL_PORTAL = registerWithoutItem("null_portal", NullPortalBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.strength(1.0F)
			.noCollision()
			.noLootTable()
			.lightLevel(state -> 11)
			.sound(SoundType.GLASS));
	/** The Chaos Arena's way back: the same look, but it cannot be broken. */
	public static final Block NULL_EXIT = registerWithoutItem("null_exit", NullPortalBlock.Exit::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.strength(-1.0F, 3600000.0F)
			.noCollision()
			.noLootTable()
			.lightLevel(state -> 11)
			.sound(SoundType.GLASS));

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, CrownOfChaos.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, CrownOfChaos.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	private static Block registerWithoutItem(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, CrownOfChaos.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
	}

	/** Loads this class so the blocks are registered. */
	public static void initialize() {
	}
}

package com.crownofchaos.entity;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;

/** The mod's entity types. */
public final class ModEntities {
	/**
	 * A Crown of Winter icicle. Registered as a falling block type so it can use the vanilla
	 * falling block renderer. Not saved with the world: icicles only last a few seconds.
	 */
	public static final EntityType<FallingBlockEntity> ICICLE = register("icicle",
			EntityType.Builder.<FallingBlockEntity>of(IcicleEntity::new, MobCategory.MISC)
					.noLootTable()
					.noSummon()
					.noSave()
					.sized(IcicleEntity.WIDTH, IcicleEntity.HEIGHT)
					.clientTrackingRange(10)
					.updateInterval(5));

	private ModEntities() {
	}

	private static <T extends FallingBlockEntity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, CrownOfChaos.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Loads this class so the entity types are registered. */
	public static void initialize() {
	}
}

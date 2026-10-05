package com.crownofchaos.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** A placed faction flag: the center of a circle of claimed territory. */
public record FactionFlag(ResourceKey<Level> dimension, BlockPos pos) {
	public static final Codec<FactionFlag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(FactionFlag::dimension),
			BlockPos.CODEC.fieldOf("pos").forGetter(FactionFlag::pos)
	).apply(instance, FactionFlag::new));

	/** Whether the spot is within {@code radius} blocks of this flag, measured across the ground (any height). */
	public boolean covers(ResourceKey<Level> dimension, double x, double z, double radius) {
		double dx = x - (this.pos.getX() + 0.5);
		double dz = z - (this.pos.getZ() + 0.5);
		return this.dimension == dimension && dx * dx + dz * dz <= radius * radius;
	}
}

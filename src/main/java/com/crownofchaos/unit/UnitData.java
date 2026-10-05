package com.crownofchaos.unit;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

/**
 * What makes a mob an army unit: who summoned it, their faction, which unit it is, the
 * faction's color (sent to players so they can tint it), and its current orders: what it was
 * told to do, the post it guards as a sentry, and the player it follows. Saved with the mob.
 */
public record UnitData(UUID owner, UUID faction, String unit, DyeColor color, UnitOrder order, Optional<BlockPos> post, Optional<UUID> leader) {
	public static final Codec<UnitData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("owner").forGetter(UnitData::owner),
			UUIDUtil.CODEC.fieldOf("faction").forGetter(UnitData::faction),
			Codec.STRING.fieldOf("unit").forGetter(UnitData::unit),
			DyeColor.CODEC.fieldOf("color").forGetter(UnitData::color),
			UnitOrder.CODEC.optionalFieldOf("order", UnitOrder.FREE).forGetter(UnitData::order),
			BlockPos.CODEC.optionalFieldOf("post").forGetter(UnitData::post),
			UUIDUtil.CODEC.optionalFieldOf("leader").forGetter(UnitData::leader)
	).apply(instance, UnitData::new));

	public static final StreamCodec<ByteBuf, UnitData> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, UnitData::owner,
			UUIDUtil.STREAM_CODEC, UnitData::faction,
			ByteBufCodecs.STRING_UTF8, UnitData::unit,
			DyeColor.STREAM_CODEC, UnitData::color,
			UnitOrder.STREAM_CODEC, UnitData::order,
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), UnitData::post,
			ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), UnitData::leader,
			UnitData::new);

	/** A new unit, with no orders. */
	public UnitData(UUID owner, UUID faction, String unit, DyeColor color) {
		this(owner, faction, unit, color, UnitOrder.FREE, Optional.empty(), Optional.empty());
	}

	public UnitData withColor(DyeColor color) {
		return new UnitData(this.owner, this.faction, this.unit, color, this.order, this.post, this.leader);
	}

	public UnitData withOrder(UnitOrder order, Optional<BlockPos> post, Optional<UUID> leader) {
		return new UnitData(this.owner, this.faction, this.unit, this.color, order, post, leader);
	}
}

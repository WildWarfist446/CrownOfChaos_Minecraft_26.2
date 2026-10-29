package com.crownofchaos.unit;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** What a unit has been told to do. */
public enum UnitOrder implements StringRepresentable {
	/** No orders: wanders like the mob normally does, passive except for the faction rules. */
	FREE("free"),
	/** Holds its post and only leaves it to fight, returning when the enemy is gone. */
	SENTRY("sentry"),
	/** Follows its leader (the player who gave the order). */
	FOLLOW("follow"),
	/** Hunts enemies nearby: other factions' players and units, wherever they are. */
	ATTACK("attack"),
	/**
	 * Holds its post like a sentry, but goes after enemy combatants (units and players it is at war with, or
	 * that its territory rules say to fight) anywhere in its faction's territory, however far, until they die
	 * or leave it, then walks back to its post.
	 */
	HUNT("hunt");

	public static final Codec<UnitOrder> CODEC = StringRepresentable.fromEnum(UnitOrder::values);
	public static final StreamCodec<ByteBuf, UnitOrder> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], UnitOrder::ordinal);

	private final String name;

	UnitOrder(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}

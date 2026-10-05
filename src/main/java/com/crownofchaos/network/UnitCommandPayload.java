package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the unit command menu or keys. The order goes to the player's selected units, or,
 * with none selected, to every unit nearby they can command (see
 * {@link com.crownofchaos.unit.UnitCommands}).
 */
public record UnitCommandPayload(Command command) implements CustomPacketPayload {
	public enum Command {
		/** Follow the player. */
		FOLLOW,
		/** Hold position where they stand now. */
		SENTRY,
		/** Hunt enemies nearby. */
		ATTACK,
		/** Drop all orders. */
		FREE,
		/** Follow, or stop following if they already all are (the follow key). */
		TOGGLE_FOLLOW,
		/** Turn back into unit items in the player's inventory. */
		RECLAIM,
		/** Select every unit nearby the player can command. */
		SELECT_NEARBY,
		/** Deselect everything. */
		CLEAR_SELECTION
	}

	public static final CustomPacketPayload.Type<UnitCommandPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("unit_command"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UnitCommandPayload> CODEC =
			ByteBufCodecs.VAR_INT.map(i -> new UnitCommandPayload(Command.values()[i]), payload -> payload.command().ordinal()).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

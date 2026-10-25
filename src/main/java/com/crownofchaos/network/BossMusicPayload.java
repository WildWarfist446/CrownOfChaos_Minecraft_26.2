package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a player's game which of the Chaos King's music to play now, replacing whatever is playing. */
public record BossMusicPayload(Mode mode) implements CustomPacketPayload {
	public enum Mode {
		/** Stop the music. */
		NONE,
		/** The fight music, on repeat until told otherwise. */
		FIGHT,
		/** The victory music, once, after he is beaten. */
		VICTORY
	}

	public static final CustomPacketPayload.Type<BossMusicPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("boss_music"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BossMusicPayload> CODEC =
			ByteBufCodecs.VAR_INT.map(i -> new BossMusicPayload(Mode.values()[i]), payload -> payload.mode().ordinal()).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

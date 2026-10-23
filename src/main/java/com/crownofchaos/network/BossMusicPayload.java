package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a player's game to start (true) or stop (false) the Chaos King's looping fight music. */
public record BossMusicPayload(boolean play) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BossMusicPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("boss_music"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BossMusicPayload> CODEC =
			ByteBufCodecs.BOOL.map(BossMusicPayload::new, BossMusicPayload::play).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

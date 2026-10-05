package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the server once it has approved an Ore Sense crown ability, telling the client to
 * highlight the ores around the player. Only that player sees the highlights, so they're drawn
 * on the client alone.
 */
public record OreSensePayload(int radius, int durationTicks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OreSensePayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("ore_sense"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OreSensePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OreSensePayload::radius,
			ByteBufCodecs.VAR_INT, OreSensePayload::durationTicks,
			OreSensePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the server once it has approved a Riptide crown ability, telling the client to
 * launch the player. Movement is client-controlled, so the launch has to run there too.
 */
public record RiptidePayload(float strength) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RiptidePayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("riptide"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RiptidePayload> CODEC =
			ByteBufCodecs.FLOAT.map(RiptidePayload::new, RiptidePayload::strength).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

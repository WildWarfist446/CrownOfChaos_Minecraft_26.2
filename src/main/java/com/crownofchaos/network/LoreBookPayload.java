package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Opens the lore book on a player's screen. Bit n of {@code unlocked} is set when page n + 1 can be read. */
public record LoreBookPayload(int unlocked) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LoreBookPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("lore_book"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LoreBookPayload> CODEC =
			ByteBufCodecs.VAR_INT.map(LoreBookPayload::new, LoreBookPayload::unlocked).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

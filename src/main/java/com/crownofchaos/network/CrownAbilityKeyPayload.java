package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the client whenever the "Use Crown Ability" key is pressed or released. A press
 * triggers the worn crown's ability; the held state is tracked for abilities that work
 * while the key is held down, such as Vein Miner.
 */
public record CrownAbilityKeyPayload(boolean pressed) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CrownAbilityKeyPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("crown_ability_key"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CrownAbilityKeyPayload> CODEC =
			ByteBufCodecs.BOOL.map(CrownAbilityKeyPayload::new, CrownAbilityKeyPayload::pressed).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

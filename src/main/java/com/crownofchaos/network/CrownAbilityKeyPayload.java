package com.crownofchaos.network;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.ability.AbilitySlot;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the client whenever a crown ability key is pressed or released. A press triggers
 * the worn crown's ability in that slot; the held state is tracked for abilities that work
 * while the key is held down.
 */
public record CrownAbilityKeyPayload(AbilitySlot slot, boolean pressed) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CrownAbilityKeyPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("crown_ability_key"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CrownAbilityKeyPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(ordinal -> AbilitySlot.values()[Math.floorMod(ordinal, AbilitySlot.values().length)], AbilitySlot::ordinal),
			CrownAbilityKeyPayload::slot,
			ByteBufCodecs.BOOL,
			CrownAbilityKeyPayload::pressed,
			CrownAbilityKeyPayload::new).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package com.crownofchaos.network;

import java.util.List;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent to a player whenever their unit selection changes: the entity ids of the selected units, so they glow. */
public record UnitSelectionPayload(List<Integer> entityIds) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UnitSelectionPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("unit_selection"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UnitSelectionPayload> CODEC =
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).map(UnitSelectionPayload::new, UnitSelectionPayload::entityIds).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

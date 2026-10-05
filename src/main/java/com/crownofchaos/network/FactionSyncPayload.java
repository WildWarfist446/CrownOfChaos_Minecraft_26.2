package com.crownofchaos.network;

import java.util.List;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.faction.Faction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the server to every player when they join and whenever any faction changes: all
 * factions, without where their flags are.
 */
public record FactionSyncPayload(List<Faction> factions) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FactionSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("faction_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionSyncPayload> CODEC =
			ByteBufCodecs.fromCodecWithRegistries(Faction.CODEC.listOf()).map(FactionSyncPayload::new, FactionSyncPayload::factions);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package com.crownofchaos.client.faction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.crownofchaos.faction.Faction;
import com.crownofchaos.network.FactionActionPayload;
import com.crownofchaos.network.FactionSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;

/**
 * The factions as last sent by the server. The faction menu reads from here and refreshes
 * when an update arrives.
 */
public final class FactionClientState {
	private static List<Faction> factions = List.of();

	private FactionClientState() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(FactionSyncPayload.TYPE, (payload, context) -> {
			factions = List.copyOf(payload.factions());
			if (context.client().gui.screen() instanceof FactionScreen screen) {
				screen.refresh();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> factions = List.of());
		// Ask for the factions as soon as we are in, so the menu never starts from an empty list.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> requestSync());
	}

	/** Asks the server to send all the factions again. */
	public static void requestSync() {
		if (ClientPlayNetworking.canSend(FactionActionPayload.TYPE)) {
			ClientPlayNetworking.send(FactionActionPayload.of(FactionActionPayload.Action.SYNC));
		}
	}

	/** Every faction, in the order they were founded. */
	public static List<Faction> all() {
		return factions;
	}

	/** The faction the local player belongs to, if any. */
	public static Optional<Faction> mine() {
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? Optional.empty() : factionOf(client.player.getUUID());
	}

	public static Optional<Faction> factionOf(UUID player) {
		return factions.stream().filter(f -> f.isMember(player)).findFirst();
	}

	public static Optional<Faction> byId(UUID id) {
		return factions.stream().filter(f -> f.id().equals(id)).findFirst();
	}
}

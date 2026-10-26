package com.crownofchaos.client;

import com.crownofchaos.network.LoreBookPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Opens the lore book screen when the server says so. */
public final class LoreBookClient {
	private LoreBookClient() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(LoreBookPayload.TYPE,
				(payload, context) -> context.client().gui.setScreen(new LoreBookScreen(payload.unlocked())));
	}
}

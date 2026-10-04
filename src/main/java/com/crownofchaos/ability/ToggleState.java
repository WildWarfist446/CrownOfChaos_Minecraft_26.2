package com.crownofchaos.ability;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * On/off state for a toggle ability, per player. Toggling shows "... enabled" in green or
 * "... disabled" in red in the action bar, from the language keys
 * {@code message.crownofchaos.ability.<name>.enabled} and {@code .disabled}. It resets to off
 * when the player leaves the server.
 */
final class ToggleState {
	private final String name;
	/** Players who have it switched on. Only touched on the server thread. */
	private final Set<UUID> enabled = new HashSet<>();

	ToggleState(String name) {
		this.name = name;
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> this.enabled.remove(handler.getPlayer().getUUID()));
	}

	/** Switches it on or off for the player and tells them which. */
	void toggle(ServerPlayer player) {
		boolean on = this.enabled.add(player.getUUID());
		if (!on) {
			this.enabled.remove(player.getUUID());
		}

		String key = "message.crownofchaos.ability." + this.name + (on ? ".enabled" : ".disabled");
		player.sendOverlayMessage(Component.translatable(key).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
	}

	boolean isEnabled(Player player) {
		return this.enabled.contains(player.getUUID());
	}
}

package com.crownofchaos.client;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Crown of Void's elytra speed. A player's own movement is decided by their game, so the boost
 * is applied here to the local player, and everyone else sees them move faster as usual.
 */
public final class VoidElytraClient {
	private VoidElytraClient() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(VoidElytraClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || !player.isFallFlying() || !CrownEffects.isWearing(player, ModItems.CROWN_OF_VOID)) {
			return;
		}
		Vec3 velocity = player.getDeltaMovement();
		if (velocity.length() < CrownEffects.VOID_CROWN_ELYTRA_MAX_SPEED) {
			player.setDeltaMovement(velocity.scale(CrownEffects.VOID_CROWN_ELYTRA_SPEED_BONUS));
		}
	}
}

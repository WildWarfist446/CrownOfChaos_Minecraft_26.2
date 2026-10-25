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
		double speed = velocity.length();
		if (speed >= CrownEffects.VOID_CROWN_ELYTRA_MAX_SPEED) {
			return;
		}
		double factor = CrownEffects.VOID_CROWN_ELYTRA_SPEED_BONUS;
		// Cruising: a glider already underway never drops below the cruise speed.
		if (speed > CrownEffects.VOID_CROWN_ELYTRA_CRUISE_MIN_START && speed * factor < CrownEffects.VOID_CROWN_ELYTRA_CRUISE_SPEED) {
			factor = Math.min(CrownEffects.VOID_CROWN_ELYTRA_CRUISE_SPEED / speed, 1.05);
		}
		player.setDeltaMovement(velocity.scale(factor));
	}
}

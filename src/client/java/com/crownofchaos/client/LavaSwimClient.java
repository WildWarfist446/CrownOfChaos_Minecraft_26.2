package com.crownofchaos.client;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Sneaking sinks you through lava when you wear the Crown of Nether, as it does in water (vanilla only
 * does that in water). The rest of the water-like movement is in {@code LivingEntityLavaMixin}.
 */
public final class LavaSwimClient {
	/** How hard sneaking pulls down each tick, the same as in water. */
	private static final double SINK_SPEED = -0.04;

	private LavaSwimClient() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer player = client.player;
			if (player != null && player.isInLava() && !player.isInWater() && player.input.keyPresses.shift() && player.isAffectedByFluids()
					&& CrownEffects.isWearingOrChaos(player, ModItems.CROWN_OF_NETHER)) {
				Vec3 velocity = player.getDeltaMovement();
				player.setDeltaMovement(velocity.x, velocity.y + SINK_SPEED, velocity.z);
			}
		});
	}
}

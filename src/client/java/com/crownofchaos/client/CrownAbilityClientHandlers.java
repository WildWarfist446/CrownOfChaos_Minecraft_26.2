package com.crownofchaos.client;

import com.crownofchaos.ability.RiptideAbility;
import com.crownofchaos.network.RiptidePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Client-side halves of crown abilities that the server can't do alone, such as moving the
 * player.
 */
public final class CrownAbilityClientHandlers {
	private CrownAbilityClientHandlers() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(RiptidePayload.TYPE, (payload, context) ->
				RiptideAbility.launch(context.player(), context.player().getItemBySlot(EquipmentSlot.HEAD), payload.strength()));
	}
}

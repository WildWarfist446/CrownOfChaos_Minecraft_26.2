package com.crownofchaos.client;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.network.CrownAbilityKeyPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;

/**
 * Registers the "Use Crown Ability" keybind (default R, rebindable in Controls) and tells the
 * server when it is pressed and released.
 */
public final class CrownAbilityKeybind {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(CrownOfChaos.id("crowns"));

	private static final KeyMapping USE_CROWN_ABILITY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.crownofchaos.use_crown_ability",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_R,
			CATEGORY));

	private static boolean wasDown;

	private CrownAbilityKeybind() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Opening a menu releases all keys, so this also reports a release then.
			boolean down = client.player != null && USE_CROWN_ABILITY.isDown();
			if (down != wasDown && ClientPlayNetworking.canSend(CrownAbilityKeyPayload.TYPE)) {
				ClientPlayNetworking.send(new CrownAbilityKeyPayload(down));
			}
			wasDown = down;
		});
	}
}

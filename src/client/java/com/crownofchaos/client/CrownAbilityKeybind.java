package com.crownofchaos.client;

import java.util.EnumMap;
import java.util.Map;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.ability.AbilitySlot;
import com.crownofchaos.network.CrownAbilityKeyPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;

/**
 * Registers the crown ability keybinds ("Use Crown Ability", default R, and "Use Secondary
 * Crown Ability", default G; both rebindable in Controls) and tells the server when each is
 * pressed and released.
 */
public final class CrownAbilityKeybind {
	/** The "Crown of Chaos" section in Controls (faction keys have their own, see {@link FactionKeybind}). */
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(CrownOfChaos.id("crowns"));

	private static final Map<AbilitySlot, KeyMapping> KEYS = new EnumMap<>(Map.of(
			AbilitySlot.PRIMARY, register("key.crownofchaos.use_crown_ability", InputConstants.KEY_R),
			AbilitySlot.SECONDARY, register("key.crownofchaos.use_secondary_crown_ability", InputConstants.KEY_G)));

	private static final Map<AbilitySlot, Boolean> WAS_DOWN = new EnumMap<>(AbilitySlot.class);

	private CrownAbilityKeybind() {
	}

	private static KeyMapping register(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (Map.Entry<AbilitySlot, KeyMapping> entry : KEYS.entrySet()) {
				AbilitySlot slot = entry.getKey();
				// Opening a menu releases all keys, so this also reports a release then.
				boolean down = client.player != null && entry.getValue().isDown();
				if (down != WAS_DOWN.getOrDefault(slot, false) && ClientPlayNetworking.canSend(CrownAbilityKeyPayload.TYPE)) {
					ClientPlayNetworking.send(new CrownAbilityKeyPayload(slot, down));
				}
				WAS_DOWN.put(slot, down);
			}
		});
	}
}

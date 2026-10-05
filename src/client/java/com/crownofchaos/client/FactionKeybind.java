package com.crownofchaos.client;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.client.faction.FactionScreen;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;

/**
 * Registers the "Faction Menu" keybind (default J; rebindable in Controls), which opens the faction
 * menu, and the "Crown of Chaos - Faction System" section in Controls that holds it and the unit
 * keys (see {@link UnitClient}).
 */
public final class FactionKeybind {
	/** The "Crown of Chaos - Faction System" section in Controls. */
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(CrownOfChaos.id("factions"));

	private static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.crownofchaos.faction_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_J, CATEGORY));

	private FactionKeybind() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (KEY.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					client.gui.setScreen(new FactionScreen());
				}
			}
		});
	}
}

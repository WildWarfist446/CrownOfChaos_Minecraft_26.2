package com.crownofchaos.menu;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** The mod's menu types. */
public final class ModMenus {
	public static final MenuType<CommanderTableMenu> COMMANDER_TABLE = Registry.register(BuiltInRegistries.MENU,
			CrownOfChaos.id("commander_table"), new MenuType<>(CommanderTableMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	/** Loads this class so the menu types are registered. */
	public static void initialize() {
	}
}

package com.crownofchaos.client;

import net.minecraft.world.item.DyeColor;

/** The loud colors that show whose army units are whose (see the unit tint and outline mixins). */
public final class UnitColors {
	private UnitColors() {
	}

	/**
	 * A loud, saturated RGB color for each faction color, picked so every one is easy to tell apart
	 * at a distance. White is plain white.
	 */
	public static int vivid(DyeColor color) {
		return switch (color) {
			case WHITE -> 0xFFFFFF;
			case ORANGE -> 0xFF7A00;
			case MAGENTA -> 0xFF00E6;
			case LIGHT_BLUE -> 0x00C8FF;
			case YELLOW -> 0xFFFF00;
			case LIME -> 0x40FF00;
			case PINK -> 0xFF7AB8;
			case GRAY -> 0x555555;
			case LIGHT_GRAY -> 0xC4C4C4;
			case CYAN -> 0x00FFC8;
			case PURPLE -> 0x8000FF;
			case BLUE -> 0x0032FF;
			case BROWN -> 0x9A5A1E;
			case GREEN -> 0x00A800;
			case RED -> 0xFF0000;
			case BLACK -> 0x1A1A1A;
		};
	}
}

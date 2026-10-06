package com.crownofchaos.client.faction;

import java.util.List;

import com.crownofchaos.client.UnitClient;
import com.crownofchaos.network.UnitCommandPayload;
import com.crownofchaos.network.UnitCommandPayload.Command;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The unit command menu (default key V): a slim, see-through strip on the right edge of the
 * screen, drawn as part of the HUD rather than as a screen. The game keeps control of the mouse,
 * camera and movement while it's open, so you can keep fighting. Each order has a number key
 * (1-7), which gives it instead of switching the hotbar (see {@link UnitClient}); the scroll
 * wheel still changes the held item. It stays open until you press V again.
 * Orders go to the selected units, or with none selected, every unit nearby you can command.
 */
public final class UnitCommandOverlay implements HudElement {
	private static final int PANEL_WIDTH = 118;
	private static final int ROW_HEIGHT = 14;
	private static final int ROW_GAP = 2;
	/** Distance from the right edge of the screen. */
	private static final int EDGE_MARGIN = 4;
	/** The panel's see-through background (ARGB): faint, so the world shows clearly behind it. */
	private static final int PANEL_BACKGROUND = 0x60140C06;
	private static final int ROW_BACKGROUND = 0x40000000;
	private static final int RED = 0xFFE05A4F;
	private static final int GREEN = 0xFF6FD66F;

	/** The orders in menu order; the number key for each is its position (1-7). */
	private static final List<Order> ORDERS = List.of(
			new Order("follow", FactionScreen.TEXT, Command.FOLLOW),
			new Order("sentry", FactionScreen.TEXT, Command.SENTRY),
			new Order("attack", RED, Command.ATTACK),
			new Order("free", FactionScreen.TEXT, Command.FREE),
			new Order("reclaim", FactionScreen.GOLD, Command.RECLAIM),
			new Order("select_nearby", GREEN, Command.SELECT_NEARBY),
			new Order("clear_selection", FactionScreen.TEXT, Command.CLEAR_SELECTION));

	private record Order(String key, int color, Command command) {
	}

	private static boolean open;

	/** How many orders there are, and so how many number keys the overlay uses. */
	public static int orderCount() {
		return ORDERS.size();
	}

	public static boolean isOpen() {
		return open;
	}

	public static void setOpen(boolean value) {
		open = value;
	}

	/** Gives the order on number key {@code index} (0-based). The overlay stays open for the next one. */
	public static void give(int index) {
		if (ClientPlayNetworking.canSend(UnitCommandPayload.TYPE)) {
			ClientPlayNetworking.send(new UnitCommandPayload(ORDERS.get(index).command()));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (!open || client.player == null) {
			return;
		}
		int panelHeight = 6 + 12 + ORDERS.size() * (ROW_HEIGHT + ROW_GAP) + 4 + 4;
		int left = graphics.guiWidth() - PANEL_WIDTH - EDGE_MARGIN;
		int top = (graphics.guiHeight() - panelHeight) / 2;
		graphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight, PANEL_BACKGROUND);
		graphics.outline(left, top, PANEL_WIDTH, panelHeight, FactionScreen.GOLD_DARK);

		int selected = UnitClient.selectedCount();
		int nearby = UnitClient.nearbyCount();
		Component status = selected > 0 ? Component.translatable("gui.crownofchaos.units.selected", selected)
				: nearby > 0 ? Component.translatable("gui.crownofchaos.units.nearby", nearby)
				: Component.translatable("gui.crownofchaos.units.none");
		int statusColor = selected + nearby > 0 ? FactionScreen.GOLD : FactionScreen.TEXT_DIM;
		graphics.centeredText(client.font, status, left + PANEL_WIDTH / 2, top + 6, statusColor);

		int rowX = left + 6;
		int y = top + 6 + 12;
		for (int i = 0; i < ORDERS.size(); i++) {
			Order order = ORDERS.get(i);
			// A small gap before the selection controls.
			if (order.command() == Command.SELECT_NEARBY) {
				y += 4;
			}
			graphics.fill(rowX, y, left + PANEL_WIDTH - 6, y + ROW_HEIGHT, ROW_BACKGROUND);
			Component label = Component.literal((i + 1) + "  ").append(Component.translatable("gui.crownofchaos.units." + order.key()));
			graphics.text(client.font, label, rowX + 4, y + (ROW_HEIGHT - 8) / 2, order.color(), true);
			y += ROW_HEIGHT + ROW_GAP;
		}

		// With nothing to command, explain how, just left of the strip.
		if (selected + nearby == 0) {
			List<FormattedCharSequence> hint = client.font.split(Component.translatable("gui.crownofchaos.units.crown_hint"), 150);
			int hintY = top + 6;
			for (FormattedCharSequence line : hint) {
				graphics.text(client.font, line, left - 154, hintY, FactionScreen.TEXT, true);
				hintY += 10;
			}
		}
	}
}

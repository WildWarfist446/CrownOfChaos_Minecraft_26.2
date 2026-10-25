package com.crownofchaos.client.faction;

import java.util.ArrayList;
import java.util.List;

import com.crownofchaos.client.UnitClient;
import com.crownofchaos.network.UnitCommandPayload;
import com.crownofchaos.network.UnitCommandPayload.Command;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The unit command menu (default key V): a slim, see-through row of orders above the hotbar and
 * its status bars, drawn as part of the HUD rather than as a screen. The game keeps control of the mouse,
 * camera and movement while it's open, so you can keep fighting. Each order has a number key
 * (1-7), which gives it instead of switching the hotbar (see {@link UnitClient}); the scroll
 * wheel still changes the held item. It stays open until you press V again.
 * Orders go to the selected units, or with none selected, every unit nearby you can command.
 */
public final class UnitCommandOverlay implements HudElement {
	private static final int CELL_HEIGHT = 14;
	private static final int CELL_PADDING = 4;
	private static final int CELL_GAP = 2;
	/**
	 * How far the cells' bottom edge is above the bottom of the screen: just above the armor row
	 * over the hotbar, leaving the same 2-pixel gap the experience bar has above the hotbar.
	 */
	private static final int STRIP_BOTTOM_OFFSET = 54;
	/** The strip's size relative to normal text, and the least space left at each side of the screen. */
	private static final float STRIP_SCALE = 0.85F;
	private static final int SCREEN_MARGIN = 8;
	/** How much smaller than normal text the status line and the hint are drawn. */
	private static final float STATUS_SCALE = 0.75F;
	private static final float HINT_SCALE = 0.45F;
	/** The hint wraps at this width (before shrinking), so it stays a few short lines. */
	private static final int HINT_WRAP_WIDTH = 420;
	/** The panel's see-through background (ARGB): faint, so the world shows clearly behind it. */
	private static final int PANEL_BACKGROUND = 0x60031A21;
	private static final int CELL_BACKGROUND = 0x40000000;
	private static final int RED = 0xFFE05A4F;
	private static final int GREEN = 0xFF6FD66F;

	/** The orders in menu order; the number key for each is its position (1-7). */
	private static final List<Order> ORDERS = List.of(
			new Order("follow", FactionScreen.TEXT, Command.FOLLOW),
			new Order("sentry", FactionScreen.TEXT, Command.SENTRY),
			new Order("attack", RED, Command.ATTACK),
			new Order("free", FactionScreen.TEXT, Command.FREE),
			new Order("reclaim", FactionScreen.ACCENT, Command.RECLAIM),
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
		Font font = client.font;
		int centerX = graphics.guiWidth() / 2;
		int bottom = graphics.guiHeight() - STRIP_BOTTOM_OFFSET;

		// The order cells, in a row centred over the hotbar, shrunk if the screen is too narrow for them.
		List<Component> labels = new ArrayList<>();
		int totalWidth = -CELL_GAP;
		for (int i = 0; i < ORDERS.size(); i++) {
			Component label = Component.literal((i + 1) + " ").append(Component.translatable("gui.crownofchaos.units." + ORDERS.get(i).key()));
			labels.add(label);
			totalWidth += font.width(label) + CELL_PADDING * 2 + CELL_GAP;
		}
		float scale = Math.min(STRIP_SCALE, (graphics.guiWidth() - 2 * SCREEN_MARGIN) / (float) (totalWidth + 6));
		int top = bottom - (int) Math.ceil(CELL_HEIGHT * scale);

		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, bottom);
		graphics.pose().scale(scale, scale);
		int left = -totalWidth / 2;
		int cellTop = -CELL_HEIGHT;
		graphics.fill(left - 3, cellTop - 3, left + totalWidth + 3, 3, PANEL_BACKGROUND);
		graphics.outline(left - 3, cellTop - 3, totalWidth + 6, CELL_HEIGHT + 6, FactionScreen.ACCENT_DARK);
		int x = left;
		for (int i = 0; i < ORDERS.size(); i++) {
			int width = font.width(labels.get(i)) + CELL_PADDING * 2;
			graphics.fill(x, cellTop, x + width, 0, CELL_BACKGROUND);
			graphics.text(font, labels.get(i), x + CELL_PADDING, cellTop + (CELL_HEIGHT - 8) / 2, ORDERS.get(i).color(), true);
			x += width + CELL_GAP;
		}
		graphics.pose().popMatrix();
		top -= (int) Math.ceil(3 * scale);

		// A small status line just above the strip, and with nothing to command, a smaller hint above that.
		int selected = UnitClient.selectedCount();
		int nearby = UnitClient.nearbyCount();
		Component status = selected > 0 ? Component.translatable("gui.crownofchaos.units.selected", selected)
				: nearby > 0 ? Component.translatable("gui.crownofchaos.units.nearby", nearby)
				: Component.translatable("gui.crownofchaos.units.none");
		int statusColor = selected + nearby > 0 ? FactionScreen.ACCENT : FactionScreen.TEXT_DIM;
		int y = top - (int) Math.ceil(8 * STATUS_SCALE) - 2;
		drawSmall(graphics, font, status.getVisualOrderText(), centerX, y, STATUS_SCALE, statusColor);
		if (selected + nearby == 0) {
			List<FormattedCharSequence> hint = font.split(Component.translatable("gui.crownofchaos.units.crown_hint"), HINT_WRAP_WIDTH);
			int lineHeight = (int) Math.ceil(8 * HINT_SCALE) + 1;
			y -= hint.size() * lineHeight + 1;
			for (FormattedCharSequence line : hint) {
				drawSmall(graphics, font, line, centerX, y, HINT_SCALE, FactionScreen.TEXT_DIM);
				y += lineHeight;
			}
		}
	}

	/** Draws text centred on {@code centerX}, shrunk by {@code scale}, with its top at {@code y}. */
	private static void drawSmall(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int centerX, int y, float scale, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, y);
		graphics.pose().scale(scale, scale);
		graphics.centeredText(font, text, 0, 0, color);
		graphics.pose().popMatrix();
	}
}

package com.crownofchaos.client.faction;

import java.util.List;

import com.crownofchaos.client.UnitClient;
import com.crownofchaos.network.UnitCommandPayload;
import com.crownofchaos.network.UnitCommandPayload.Command;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The unit command menu (default key V): a slim, see-through strip on the right edge of the
 * screen, so the fight stays in view. The world isn't dimmed or blurred behind it. Each order
 * has a number key (1-7) for giving it without aiming the mouse; pressing V again closes it.
 * Orders go to the selected units, or with none selected, every unit nearby you can command.
 * Each button sends a {@link UnitCommandPayload} and closes the menu, so the result shows in
 * the action bar.
 */
public class UnitCommandScreen extends Screen {
	private static final int PANEL_WIDTH = 118;
	private static final int BUTTON_WIDTH = 106;
	private static final int BUTTON_HEIGHT = 14;
	private static final int BUTTON_GAP = 2;
	/** Distance from the right edge of the screen. */
	private static final int EDGE_MARGIN = 4;
	/** The panel's see-through background (ARGB): faint, so the world shows clearly behind it. */
	private static final int PANEL_BACKGROUND = 0x60140C06;
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

	private int left;
	private int top;
	private int panelHeight;

	public UnitCommandScreen() {
		super(Component.translatable("gui.crownofchaos.units.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		this.panelHeight = 6 + 12 + ORDERS.size() * (BUTTON_HEIGHT + BUTTON_GAP) + 4 + 4;
		this.left = this.width - PANEL_WIDTH - EDGE_MARGIN;
		this.top = (this.height - this.panelHeight) / 2;
		int x = this.left + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
		int y = this.top + 6 + 12;
		for (int i = 0; i < ORDERS.size(); i++) {
			Order order = ORDERS.get(i);
			// A small gap before the selection controls.
			if (order.command() == Command.SELECT_NEARBY) {
				y += 4;
			}
			Component label = Component.literal((i + 1) + "  ").append(Component.translatable("gui.crownofchaos.units." + order.key()));
			this.addRenderableWidget(new FactionButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, order.color(), () -> this.give(order.command()))
					.setTranslucent(true));
			y += BUTTON_HEIGHT + BUTTON_GAP;
		}
	}

	private void give(Command command) {
		if (ClientPlayNetworking.canSend(UnitCommandPayload.TYPE)) {
			ClientPlayNetworking.send(new UnitCommandPayload(command));
		}
		this.onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.input();
		if (key >= InputConstants.KEY_1 && key < InputConstants.KEY_1 + ORDERS.size()) {
			this.give(ORDERS.get(key - InputConstants.KEY_1).command());
			return true;
		}
		if (UnitClient.isCommandKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	/** No dimming or blur: just a faint panel, so the fight stays visible. */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int right = this.left + PANEL_WIDTH;
		int bottom = this.top + this.panelHeight;
		graphics.fill(this.left, this.top, right, bottom, PANEL_BACKGROUND);
		graphics.outline(this.left, this.top, PANEL_WIDTH, this.panelHeight, FactionScreen.GOLD_DARK);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int selected = UnitClient.selectedCount();
		int nearby = UnitClient.nearbyCount();
		Component status = selected > 0 ? Component.translatable("gui.crownofchaos.units.selected", selected)
				: nearby > 0 ? Component.translatable("gui.crownofchaos.units.nearby", nearby)
				: Component.translatable("gui.crownofchaos.units.none");
		int color = selected + nearby > 0 ? FactionScreen.GOLD : FactionScreen.TEXT_DIM;
		graphics.centeredText(this.font, status, this.left + PANEL_WIDTH / 2, this.top + 6, color);

		// With nothing to command, explain how, just left of the strip.
		if (selected + nearby == 0) {
			List<FormattedCharSequence> hint = this.font.split(Component.translatable("gui.crownofchaos.units.crown_hint"), 150);
			int y = this.top + 6;
			for (FormattedCharSequence line : hint) {
				graphics.text(this.font, line, this.left - 154, y, FactionScreen.TEXT, true);
				y += 10;
			}
		}
	}
}

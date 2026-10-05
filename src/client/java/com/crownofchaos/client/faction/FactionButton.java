package com.crownofchaos.client.faction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * A flat button drawn with fills and outlines in the faction menu's colors, so the menu needs no
 * textures. Tabs use the same button with {@link #setSelected}.
 */
public class FactionButton extends AbstractButton {
	private static final int FILL = 0xFF4A3420;
	private static final int FILL_HOVER = 0xFF5E4329;
	private static final int FILL_SELECTED = 0xFF6B4E2A;
	private static final int FILL_INACTIVE = 0xFF33241A;
	private static final int TEXT_INACTIVE = 0xFF7A6A55;

	private final Runnable onPress;
	private final int textColor;
	private boolean selected;

	public FactionButton(int x, int y, int width, int height, Component message, int textColor, Runnable onPress) {
		super(x, y, width, height, message);
		this.textColor = textColor;
		this.onPress = onPress;
	}

	/** Marks a tab as the open one: it's drawn highlighted and ignores clicks. */
	public FactionButton setSelected(boolean selected) {
		this.selected = selected;
		return this;
	}

	public FactionButton setActive(boolean active) {
		this.active = active;
		return this;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		if (!this.selected) {
			this.onPress.run();
		}
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		int h = this.getHeight();
		boolean hovered = this.active && this.isHoveredOrFocused() && !this.selected;
		int fill = !this.active ? FILL_INACTIVE : this.selected ? FILL_SELECTED : hovered ? FILL_HOVER : FILL;
		int border = !this.active ? FactionScreen.GOLD_DARK & 0x80FFFFFF
				: this.selected || hovered ? FactionScreen.GOLD : FactionScreen.GOLD_DARK;
		graphics.fill(x, y, x + w, y + h, fill);
		graphics.outline(x, y, w, h, border);

		Font font = Minecraft.getInstance().font;
		int textY = y + (h - 8) / 2;
		if (this.active) {
			int color = this.selected ? FactionScreen.GOLD : this.textColor;
			graphics.centeredText(font, this.getMessage(), x + w / 2, textY, color);
		} else {
			// Plain text so colored labels turn gray too.
			graphics.centeredText(font, this.getMessage().getString(), x + w / 2, textY, TEXT_INACTIVE);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

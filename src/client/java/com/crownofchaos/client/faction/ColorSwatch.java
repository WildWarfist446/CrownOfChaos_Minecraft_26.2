package com.crownofchaos.client.faction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

/**
 * A square of one dye color for picking a faction color. Taken colors are dimmed and crossed out
 * and can't be picked; the chosen one gets a gold frame. Hovering shows the color's name.
 */
public class ColorSwatch extends AbstractButton {
	private final DyeColor color;
	private final boolean selected;
	private final boolean taken;
	private final Runnable onPick;

	public ColorSwatch(int x, int y, int size, DyeColor color, boolean selected, Component takenBy, Runnable onPick) {
		super(x, y, size, size, colorName(color));
		this.color = color;
		this.selected = selected;
		this.taken = takenBy != null;
		this.onPick = onPick;
		Component tip = this.taken
				? colorName(color).copy().append("\n").append(Component.translatable("gui.crownofchaos.faction.color.taken", takenBy))
				: colorName(color);
		this.setTooltip(Tooltip.create(tip));
	}

	public static Component colorName(DyeColor color) {
		return Component.translatable("color.minecraft." + color.getName());
	}

	public static int argb(DyeColor color) {
		return 0xFF000000 | color.getTextColor();
	}

	@Override
	public void onPress(InputWithModifiers input) {
		if (!this.taken && !this.selected) {
			this.onPick.run();
		}
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = this.getX();
		int y = this.getY();
		int s = this.getWidth();
		int frame = this.selected ? FactionScreen.ACCENT
				: this.isHoveredOrFocused() && !this.taken ? FactionScreen.TEXT : 0xFF03181E;
		graphics.fill(x, y, x + s, y + s, frame);
		graphics.fill(x + 1, y + 1, x + s - 1, y + s - 1, this.selected ? 0xFF03181E : frame);
		graphics.fill(x + 2, y + 2, x + s - 2, y + s - 2, argb(this.color));
		if (this.taken) {
			graphics.fill(x + 2, y + 2, x + s - 2, y + s - 2, 0xA0000000);
			// A cross drawn pixel by pixel.
			for (int i = 3; i < s - 3; i++) {
				graphics.fill(x + i, y + i, x + i + 1, y + i + 1, 0xFFE04040);
				graphics.fill(x + s - 1 - i, y + i, x + s - i, y + i + 1, 0xFFE04040);
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

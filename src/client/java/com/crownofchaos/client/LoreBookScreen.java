package com.crownofchaos.client;

import java.util.List;

import com.crownofchaos.client.faction.FactionScreen;
import com.crownofchaos.lore.LoreBook;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * The lore book, laid out like the vanilla one (one page at a time, a page counter, arrows to turn)
 * but drawn in the mod's teal and cyan like the faction menus. Glitched words, and every page the
 * player has not unlocked, use the scrambling text style, so they keep changing.
 */
public final class LoreBookScreen extends Screen {
	private static final int PANEL_WIDTH = 240;
	private static final int PANEL_HEIGHT = 180;
	private static final int MARGIN = 16;
	private static final int ARROW_WIDTH = 24;
	private static final int ARROW_HEIGHT = 14;

	private final int unlocked;
	private int page;
	private int left;
	private int top;

	public LoreBookScreen(int unlocked) {
		super(Component.translatable("lore.crownofchaos.title"));
		this.unlocked = unlocked;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		this.left = (this.width - PANEL_WIDTH) / 2;
		this.top = (this.height - PANEL_HEIGHT) / 2;
	}

	private boolean isUnlocked(int index) {
		return (this.unlocked >> index & 1) != 0;
	}

	/** The page's text: glitched words ([[like this]]) scrambled, and the whole page scrambled if it is locked. */
	private Component pageText(int index) {
		String raw = Language.getInstance().getOrDefault("lore.crownofchaos.page." + (index + 1));
		boolean locked = !this.isUnlocked(index);
		MutableComponent text = Component.empty();
		int at = 0;
		while (at < raw.length()) {
			int open = raw.indexOf("[[", at);
			int close = open < 0 ? -1 : raw.indexOf("]]", open);
			if (open < 0 || close < 0) {
				text.append(segment(raw.substring(at), locked));
				break;
			}
			text.append(segment(raw.substring(at, open), locked));
			text.append(segment(raw.substring(open + 2, close), true));
			at = close + 2;
		}
		return text;
	}

	private static Component segment(String text, boolean scrambled) {
		MutableComponent component = Component.literal(text);
		return scrambled ? component.withStyle(ChatFormatting.OBFUSCATED) : component;
	}

	private int arrowY() {
		return this.top + PANEL_HEIGHT - MARGIN - ARROW_HEIGHT + 4;
	}

	private boolean overPrevious(double x, double y) {
		return this.page > 0 && x >= this.left + MARGIN && x < this.left + MARGIN + ARROW_WIDTH && y >= this.arrowY() && y < this.arrowY() + ARROW_HEIGHT;
	}

	private boolean overNext(double x, double y) {
		int right = this.left + PANEL_WIDTH - MARGIN;
		return this.page < LoreBook.PAGE_COUNT - 1 && x >= right - ARROW_WIDTH && x < right && y >= this.arrowY() && y < this.arrowY() + ARROW_HEIGHT;
	}

	private void turn(int by) {
		this.page = Math.max(0, Math.min(LoreBook.PAGE_COUNT - 1, this.page + by));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.overPrevious(event.x(), event.y())) {
			this.turn(-1);
			return true;
		}
		if (this.overNext(event.x(), event.y())) {
			this.turn(1);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isLeft() || event.key() == 65 || event.key() == 266) {
			this.turn(-1);
			return true;
		}
		if (event.isRight() || event.key() == 68 || event.key() == 267) {
			this.turn(1);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		this.turn(scrollY < 0 ? 1 : -1);
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractTransparentBackground(graphics);
		int x0 = this.left;
		int y0 = this.top;
		int x1 = x0 + PANEL_WIDTH;
		int y1 = y0 + PANEL_HEIGHT;
		graphics.fill(x0, y0, x1, y1, FactionScreen.PANEL_OUTER);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, FactionScreen.PANEL_INNER);
		graphics.outline(x0, y0, PANEL_WIDTH, PANEL_HEIGHT, FactionScreen.ACCENT);
		graphics.outline(x0 + 2, y0 + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, FactionScreen.ACCENT_DARK);
		for (int[] c : new int[][] {{x0 + 4, y0 + 4}, {x1 - 7, y0 + 4}, {x0 + 4, y1 - 7}, {x1 - 7, y1 - 7}}) {
			graphics.fill(c[0], c[1], c[0] + 3, c[1] + 3, FactionScreen.ACCENT);
		}
		graphics.fill(x0 + 8, y0 + 22, x1 - 8, y0 + 23, FactionScreen.ACCENT_DARK);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.left + PANEL_WIDTH / 2, this.top + 8, FactionScreen.ACCENT);

		List<FormattedCharSequence> lines = this.font.split(this.pageText(this.page), PANEL_WIDTH - MARGIN * 2);
		int y = this.top + 30;
		for (FormattedCharSequence line : lines) {
			graphics.text(this.font, line, this.left + MARGIN, y, FactionScreen.TEXT);
			y += this.font.lineHeight + 2;
		}

		graphics.centeredText(this.font, Component.translatable("lore.crownofchaos.page_counter", this.page + 1, LoreBook.PAGE_COUNT),
				this.left + PANEL_WIDTH / 2, this.arrowY() + 3, FactionScreen.TEXT_DIM);
		this.drawArrow(graphics, mouseX, mouseY, this.left + MARGIN, "<", this.overPrevious(mouseX, mouseY), this.page > 0);
		this.drawArrow(graphics, mouseX, mouseY, this.left + PANEL_WIDTH - MARGIN - ARROW_WIDTH, ">", this.overNext(mouseX, mouseY), this.page < LoreBook.PAGE_COUNT - 1);
	}

	private void drawArrow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, String label, boolean hovered, boolean active) {
		if (!active) {
			return;
		}
		int y = this.arrowY();
		graphics.fill(x, y, x + ARROW_WIDTH, y + ARROW_HEIGHT, hovered ? 0xFF0F7480 : 0xFF0A5A63);
		graphics.outline(x, y, ARROW_WIDTH, ARROW_HEIGHT, hovered ? FactionScreen.ACCENT : FactionScreen.ACCENT_DARK);
		graphics.centeredText(this.font, label, x + ARROW_WIDTH / 2, y + 3, FactionScreen.TEXT);
	}
}

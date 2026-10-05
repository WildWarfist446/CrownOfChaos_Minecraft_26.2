package com.crownofchaos.client.menu;

import java.util.List;
import java.util.Optional;

import com.crownofchaos.menu.CommanderTableMenu;
import com.crownofchaos.unit.UnitType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Commander's Table screen. Put a crown in the crown slot, pick one of the units it commands
 * from the list, put the unit's two cost items in the cost slots and take the unit item.
 *
 * <p>Everything is drawn with flat fills and outlines (no textures), in the same style as the
 * faction menu. Picking a unit works like the stonecutter: the click is sent to the server with
 * {@code handleInventoryButtonClick}, which selects it in the menu.
 */
public class CommanderTableScreen extends AbstractContainerScreen<CommanderTableMenu> {
	// Colors (ARGB), matching the faction menu.
	private static final int PANEL_OUTER = 0xF02A1B10;
	private static final int PANEL_INNER = 0xF03A2715;
	private static final int GOLD = 0xFFD4AF37;
	private static final int GOLD_DARK = 0xFF8B6F1E;
	private static final int TEXT = 0xFFEFE3C2;
	private static final int TEXT_DIM = 0xFFB3A27F;
	private static final int SLOT_BORDER = 0xFF6B5320;
	private static final int SLOT_FILL = 0xFF1E140B;
	private static final int LIST_FILL = 0x70120A04;
	private static final int ENTRY_FILL = 0x40000000;
	private static final int ENTRY_HOVER_FILL = 0x40FFF2D0;
	private static final int ENTRY_SELECTED_FILL = 0x60D4AF37;
	/** The faint tint drawn behind and over a ghost cost item (the same values vanilla's recipe book uses). */
	private static final int GHOST_BACK = 0x30FF0000;
	private static final int GHOST_FRONT = 0x30FFFFFF;

	// Unit list layout, inside the menu's unit area.
	private static final int LIST_COLUMNS = 2;
	private static final int LIST_VISIBLE_ROWS = 2;
	private static final int ENTRY_HEIGHT = 16;
	private static final int ENTRY_GAP = 1;
	private static final int LIST_PADDING = 1;
	private static final int SCROLLBAR_WIDTH = 3;
	/** The line under the unit grid (still inside the unit area) with the chosen unit's name. */
	private static final int NAME_LINE_Y = CommanderTableMenu.UNITS_Y + 37;
	/** The status line between the unit area and the cost slots. */
	private static final int STATUS_LINE_Y = CommanderTableMenu.UNITS_Y + CommanderTableMenu.UNITS_HEIGHT + 3;

	private static final String KEY = "gui.crownofchaos.commander_table.";

	/** The first visible row of the unit list, when there are more units than fit. */
	private int startRow;
	private List<UnitType> shownUnits = List.of();

	public CommanderTableScreen(CommanderTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, CommanderTableMenu.IMAGE_WIDTH, CommanderTableMenu.IMAGE_HEIGHT);
		this.inventoryLabelY = CommanderTableMenu.INVENTORY_Y - 11;
		menu.registerUpdateListener(this::containerChanged);
	}

	private static Component tr(String key) {
		return Component.translatable(KEY + key);
	}

	private void containerChanged() {
		// A different crown means a different list: go back to the top.
		if (this.menu.units() != this.shownUnits) {
			this.shownUnits = this.menu.units();
			this.startRow = 0;
		}
	}

	// ---------------------------------------------------------------- unit list geometry

	private int unitCount() {
		return this.menu.units().size();
	}

	private int totalRows() {
		return (this.unitCount() + this.columns() - 1) / this.columns();
	}

	private boolean canScroll() {
		return this.totalRows() > LIST_VISIBLE_ROWS;
	}

	private int maxStartRow() {
		return Math.max(0, this.totalRows() - LIST_VISIBLE_ROWS);
	}

	/** One wide column when every unit fits that way (so names aren't cut short), otherwise two. */
	private int columns() {
		return this.unitCount() <= LIST_VISIBLE_ROWS ? 1 : LIST_COLUMNS;
	}

	private int entryWidth() {
		int width = CommanderTableMenu.UNITS_WIDTH - LIST_PADDING * 2 - (this.canScroll() ? SCROLLBAR_WIDTH + 1 : 0);
		return (width - ENTRY_GAP * (this.columns() - 1)) / this.columns();
	}

	/** The screen x of a visible entry (by its position among the visible ones). */
	private int entryX(int visibleIndex) {
		return this.leftPos + CommanderTableMenu.UNITS_X + LIST_PADDING + visibleIndex % this.columns() * (this.entryWidth() + ENTRY_GAP);
	}

	private int entryY(int visibleIndex) {
		return this.topPos + CommanderTableMenu.UNITS_Y + LIST_PADDING + visibleIndex / this.columns() * (ENTRY_HEIGHT + ENTRY_GAP);
	}

	private int firstVisible() {
		this.startRow = Math.clamp(this.startRow, 0, this.maxStartRow());
		return this.startRow * this.columns();
	}

	private int lastVisible() {
		return Math.min(this.unitCount(), this.firstVisible() + LIST_VISIBLE_ROWS * this.columns());
	}

	/** The index into {@code menu.units()} of the entry under the mouse, or -1. */
	private int entryAt(double mouseX, double mouseY) {
		int width = this.entryWidth();
		int first = this.firstVisible();
		for (int index = first; index < this.lastVisible(); index++) {
			int x = this.entryX(index - first);
			int y = this.entryY(index - first);
			if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ENTRY_HEIGHT) {
				return index;
			}
		}
		return -1;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x0 = this.leftPos;
		int y0 = this.topPos;
		int x1 = x0 + this.imageWidth;
		int y1 = y0 + this.imageHeight;

		// Panel with a double gold border and studs in the corners.
		graphics.fill(x0, y0, x1, y1, PANEL_OUTER);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, PANEL_INNER);
		graphics.outline(x0, y0, this.imageWidth, this.imageHeight, GOLD);
		graphics.outline(x0 + 2, y0 + 2, this.imageWidth - 4, this.imageHeight - 4, GOLD_DARK);
		for (int[] c : new int[][] {{x0 + 4, y0 + 4}, {x1 - 7, y0 + 4}, {x0 + 4, y1 - 7}, {x1 - 7, y1 - 7}}) {
			graphics.fill(c[0], c[1], c[0] + 3, c[1] + 3, GOLD);
		}

		// A slot background behind every slot; the crown and result slots get an extra gold frame.
		for (Slot slot : this.menu.slots) {
			int sx = x0 + slot.x;
			int sy = y0 + slot.y;
			if (slot.index == CommanderTableMenu.CROWN_SLOT || slot.index == CommanderTableMenu.RESULT_SLOT) {
				graphics.outline(sx - 3, sy - 3, 22, 22, GOLD_DARK);
			}
			graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BORDER);
			graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_FILL);
		}

		this.drawArrow(graphics);
		this.drawUnitList(graphics, mouseX, mouseY);
	}

	/** An arrow from the cost slots to the result slot, bright gold when the unit can be made. */
	private void drawArrow(GuiGraphicsExtractor graphics) {
		int color = this.menu.canAfford() ? GOLD : GOLD_DARK;
		int left = this.leftPos + CommanderTableMenu.COST_B_X + 16 + 7;
		int right = this.leftPos + CommanderTableMenu.RESULT_X - 3 - 6;
		int midY = this.topPos + CommanderTableMenu.COST_Y + 8;
		int headLength = 6;
		graphics.fill(left, midY - 1, right - headLength, midY + 1, color);
		for (int i = 0; i < headLength; i++) {
			int x = right - headLength + i;
			int half = headLength - i;
			graphics.fill(x, midY - half, x + 1, midY + half, color);
		}
	}

	private void drawUnitList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int ax = this.leftPos + CommanderTableMenu.UNITS_X;
		int ay = this.topPos + CommanderTableMenu.UNITS_Y;
		int aw = CommanderTableMenu.UNITS_WIDTH;
		int ah = CommanderTableMenu.UNITS_HEIGHT;
		graphics.fill(ax, ay, ax + aw, ay + ah, LIST_FILL);
		graphics.outline(ax - 1, ay - 1, aw + 2, ah + 2, GOLD_DARK);

		List<UnitType> units = this.menu.units();
		if (units.isEmpty()) {
			return;
		}
		int width = this.entryWidth();
		int first = this.firstVisible();
		int hovered = this.entryAt(mouseX, mouseY);
		for (int index = first; index < this.lastVisible(); index++) {
			UnitType unit = units.get(index);
			int x = this.entryX(index - first);
			int y = this.entryY(index - first);
			boolean selected = index == this.menu.selectedIndex();
			if (selected) {
				graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, ENTRY_SELECTED_FILL);
				graphics.outline(x, y, width, ENTRY_HEIGHT, GOLD);
			} else if (index == hovered) {
				graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, ENTRY_HOVER_FILL);
				graphics.outline(x, y, width, ENTRY_HEIGHT, TEXT_DIM);
			} else {
				graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, ENTRY_FILL);
				graphics.outline(x, y, width, ENTRY_HEIGHT, 0x60D4AF37);
			}
			if (index == hovered) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
			graphics.item(new ItemStack(unit.item()), x, y);
			int textSpace = width - 20;
			if (textSpace > 8) {
				graphics.text(this.font, this.fit(unit.entity().getDescription(), textSpace), x + 18, y + 4,
						selected ? GOLD : TEXT, true);
			}
		}

		if (this.canScroll()) {
			int trackX = ax + aw - LIST_PADDING - SCROLLBAR_WIDTH;
			int trackY = ay + LIST_PADDING;
			int trackHeight = LIST_VISIBLE_ROWS * (ENTRY_HEIGHT + ENTRY_GAP) - ENTRY_GAP;
			graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0x60000000);
			int thumbHeight = Math.max(4, trackHeight * LIST_VISIBLE_ROWS / this.totalRows());
			int thumbY = trackY + (trackHeight - thumbHeight) * this.startRow / Math.max(1, this.maxStartRow());
			graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, GOLD);
		}
	}

	/** Drawn relative to the panel's top left corner. */
	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.centeredText(this.font, this.title, this.imageWidth / 2, 6, GOLD);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_DIM, false);

		// "Crown" above the crown slot, "Cost" beside the cost slots.
		graphics.centeredText(this.font, tr("crown"), CommanderTableMenu.CROWN_X + 8, CommanderTableMenu.CROWN_Y - 12, GOLD);
		Component cost = tr("cost");
		graphics.text(this.font, cost, CommanderTableMenu.COST_A_X - 5 - this.font.width(cost), CommanderTableMenu.COST_Y + 4, GOLD, true);

		int areaCenter = CommanderTableMenu.UNITS_X + CommanderTableMenu.UNITS_WIDTH / 2;
		int areaWidth = CommanderTableMenu.UNITS_WIDTH - 6;
		if (!this.menu.hasCrown() || this.menu.units().isEmpty()) {
			// A hint in the middle of the empty list.
			Component hint = tr(this.menu.hasCrown() ? "no_units" : "place_crown");
			List<FormattedCharSequence> lines = this.font.split(hint, areaWidth);
			int y = CommanderTableMenu.UNITS_Y + (CommanderTableMenu.UNITS_HEIGHT - lines.size() * 10) / 2 + 1;
			for (FormattedCharSequence line : lines) {
				graphics.centeredText(this.font, line, areaCenter, y, TEXT_DIM);
				y += 10;
			}
			return;
		}

		Optional<UnitType> selected = this.menu.selectedUnit();
		if (selected.isPresent()) {
			Component name = new ItemStack(selected.get().item()).getHoverName();
			graphics.centeredText(this.font, this.fit(name, areaWidth), areaCenter, NAME_LINE_Y, GOLD);
			if (!this.menu.canAfford()) {
				graphics.centeredText(this.font, this.fit(tr("add_cost"), areaWidth), areaCenter, STATUS_LINE_Y, TEXT_DIM);
			}
		} else {
			graphics.centeredText(this.font, this.fit(tr("pick_unit"), areaWidth), areaCenter, NAME_LINE_Y, TEXT_DIM);
		}
	}

	/** Draws the slots, then faint "ghost" cost items in the empty cost slots. Relative to the panel. */
	@Override
	protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractSlots(graphics, mouseX, mouseY);
		for (int slotIndex : new int[] {CommanderTableMenu.COST_A_SLOT, CommanderTableMenu.COST_B_SLOT}) {
			Slot slot = this.menu.getSlot(slotIndex);
			ItemStack ghost = this.ghostFor(slot);
			if (!ghost.isEmpty()) {
				graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, GHOST_BACK);
				graphics.fakeItem(ghost, slot.x, slot.y);
				graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, GHOST_FRONT);
				graphics.itemDecorations(this.font, ghost, slot.x, slot.y);
			}
		}
	}

	/**
	 * The cost item to show faintly in an empty cost slot, or empty. Slot A shows the first cost and
	 * slot B the second, unless the other slot already holds one of them, then it shows the other.
	 */
	private ItemStack ghostFor(Slot slot) {
		if (slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		Optional<UnitType> selected = this.menu.selectedUnit();
		if (selected.isEmpty()) {
			return ItemStack.EMPTY;
		}
		UnitType unit = selected.get();
		boolean isA = slot.index == CommanderTableMenu.COST_A_SLOT;
		ItemStack other = this.menu.getSlot(isA ? CommanderTableMenu.COST_B_SLOT : CommanderTableMenu.COST_A_SLOT).getItem();
		UnitType.Cost own = isA ? unit.costA() : unit.costB();
		UnitType.Cost theirs = isA ? unit.costB() : unit.costA();
		if (!other.isEmpty() && other.is(own.item()) && !other.is(theirs.item())) {
			return theirs.toStack();
		}
		return own.toStack();
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (!this.menu.getCarried().isEmpty()) {
			return;
		}
		// Ghost cost items.
		if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
			ItemStack ghost = this.ghostFor(this.hoveredSlot);
			if (!ghost.isEmpty()) {
				graphics.setComponentTooltipForNextFrame(this.font, getTooltipFromItem(this.minecraft, ghost), mouseX, mouseY,
						(Identifier) ghost.get(DataComponents.TOOLTIP_STYLE));
				return;
			}
		}
		// Unit list entries: the unit's name.
		int index = this.entryAt(mouseX, mouseY);
		if (index >= 0) {
			UnitType unit = this.menu.units().get(index);
			graphics.setTooltipForNextFrame(this.font, new ItemStack(unit.item()).getHoverName(), mouseX, mouseY);
		}
	}

	/** The text, cut short with "..." if it's wider than {@code maxWidth}. */
	private FormattedCharSequence fit(Component text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text.getVisualOrderText();
		}
		int ellipsis = this.font.width("...");
		FormattedText cut = this.font.substrByWidth(text, Math.max(0, maxWidth - ellipsis));
		return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("...")));
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			int index = this.entryAt(event.x(), event.y());
			if (index >= 0 && this.menu.clickMenuButton(this.minecraft.player, index)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (super.mouseScrolled(x, y, scrollX, scrollY)) {
			return true;
		}
		int ax = this.leftPos + CommanderTableMenu.UNITS_X;
		int ay = this.topPos + CommanderTableMenu.UNITS_Y;
		boolean overList = x >= ax && x < ax + CommanderTableMenu.UNITS_WIDTH && y >= ay && y < ay + CommanderTableMenu.UNITS_HEIGHT;
		if (overList && this.canScroll() && scrollY != 0) {
			this.startRow = Math.clamp(this.startRow - (int) Math.signum(scrollY), 0, this.maxStartRow());
			return true;
		}
		return false;
	}
}

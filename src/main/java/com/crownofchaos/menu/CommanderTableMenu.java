package com.crownofchaos.menu;

import java.util.List;
import java.util.Optional;

import com.crownofchaos.block.ModBlocks;
import com.crownofchaos.item.ModItems;
import com.crownofchaos.unit.ModUnits;
import com.crownofchaos.unit.UnitType;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Commander's Table menu. Put a crown in the crown slot to see the units it commands, pick
 * one, and put its two cost items in the cost slots (in either order); the unit item then
 * appears in the result slot. A crown you are wearing counts too, when the crown slot is empty. Taking it uses up the cost items; the crown stays. Like a
 * crafting table, the table keeps nothing: closing the menu gives the items back.
 *
 * <p>Selecting a unit works like the stonecutter: the screen calls
 * {@code gameMode.handleInventoryButtonClick(containerId, index)}, which reaches
 * {@link #clickMenuButton} on the server, and the selection is synced back in a data slot.
 */
public class CommanderTableMenu extends AbstractContainerMenu {
	public static final int CROWN_SLOT = 0;
	public static final int COST_A_SLOT = 1;
	public static final int COST_B_SLOT = 2;
	public static final int RESULT_SLOT = 3;
	private static final int INV_SLOT_START = 4;
	private static final int INV_SLOT_END = 31;
	private static final int HOTBAR_SLOT_END = 40;

	/** The screen's size and where everything sits in it, so the screen can draw around the slots. */
	public static final int IMAGE_WIDTH = 176;
	public static final int IMAGE_HEIGHT = 186;
	public static final int CROWN_X = 14;
	public static final int CROWN_Y = 34;
	public static final int COST_A_X = 52;
	public static final int COST_B_X = 76;
	public static final int COST_Y = 74;
	public static final int RESULT_X = 136;
	public static final int RESULT_Y = 74;
	public static final int INVENTORY_X = 8;
	public static final int INVENTORY_Y = 104;
	/** The area of the screen for the unit list (x, y, width, height). */
	public static final int UNITS_X = 40;
	public static final int UNITS_Y = 16;
	public static final int UNITS_WIDTH = 128;
	public static final int UNITS_HEIGHT = 46;

	private final ContainerLevelAccess access;
	private final Player player;
	private final DataSlot selectedUnit = DataSlot.standalone();
	private final Slot crownSlot;
	private final Slot costASlot;
	private final Slot costBSlot;
	private final Slot resultSlot;
	private List<UnitType> units = List.of();
	private ItemStack lastCrown = ItemStack.EMPTY;
	private long lastSoundTime;
	private Runnable slotUpdateListener = () -> {};

	private final Container inputs = new SimpleContainer(3) {
		@Override
		public void setChanged() {
			super.setChanged();
			CommanderTableMenu.this.slotsChanged(this);
			CommanderTableMenu.this.slotUpdateListener.run();
		}
	};
	private final ResultContainer result = new ResultContainer();

	/** Client side: opened from the server's menu. */
	public CommanderTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public CommanderTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(ModMenus.COMMANDER_TABLE, containerId);
		this.access = access;
		this.player = inventory.player;
		this.crownSlot = this.addSlot(new Slot(this.inputs, 0, CROWN_X, CROWN_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ModItems.isCrown(stack.getItem());
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.costASlot = this.addSlot(new Slot(this.inputs, 1, COST_A_X, COST_Y));
		this.costBSlot = this.addSlot(new Slot(this.inputs, 2, COST_B_X, COST_Y));
		this.resultSlot = this.addSlot(new Slot(this.result, 3, RESULT_X, RESULT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack carried) {
				carried.onCraftedBy(player, carried.getCount());
				CommanderTableMenu.this.payForUnit();
				access.execute((level, pos) -> {
					long gameTime = level.getGameTime();
					if (CommanderTableMenu.this.lastSoundTime != gameTime) {
						level.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
						CommanderTableMenu.this.lastSoundTime = gameTime;
					}
				});
				super.onTake(player, carried);
			}
		});
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
		this.selectedUnit.set(-1);
		this.addDataSlot(this.selectedUnit);
		// A crown already on your head lists its units from the moment the table opens.
		this.refreshUnits();
	}

	// For the screen

	/** The units the crown in the crown slot commands (empty without a crown, or for a crown with no units). */
	public List<UnitType> units() {
		return this.units;
	}

	/** The index into {@link #units()} of the chosen unit, or -1 for none. */
	public int selectedIndex() {
		return this.selectedUnit.get();
	}

	public Optional<UnitType> selectedUnit() {
		int index = this.selectedUnit.get();
		return index >= 0 && index < this.units.size() ? Optional.of(this.units.get(index)) : Optional.empty();
	}

	/** Whether there is a crown to list units for: one in the crown slot, or else the one you are wearing. */
	public boolean hasCrown() {
		return !this.effectiveCrown().isEmpty();
	}

	/** The crown in the crown slot, or, with that empty, the crown on the player's head (if it is one). */
	private ItemStack effectiveCrown() {
		ItemStack inSlot = this.crownSlot.getItem();
		if (!inSlot.isEmpty()) {
			return inSlot;
		}
		ItemStack worn = this.player.getItemBySlot(EquipmentSlot.HEAD);
		return ModItems.isCrown(worn.getItem()) ? worn : ItemStack.EMPTY;
	}

	/** Whether the cost slots hold enough of the chosen unit's two cost items (in either order). */
	public boolean canAfford() {
		return this.selectedUnit().map(this::isPaid).orElse(false);
	}

	/** Called whenever the input slots change, so the screen can update. */
	public void registerUpdateListener(Runnable listener) {
		this.slotUpdateListener = listener;
	}

	// Logic

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId < 0 || buttonId >= this.units.size() || buttonId == this.selectedUnit.get()) {
			return false;
		}
		this.selectedUnit.set(buttonId);
		this.updateResult();
		return true;
	}

	@Override
	public void slotsChanged(Container container) {
		this.refreshUnits();
		this.updateResult();
	}

	/** Lists the units of the crown in use when that crown changes. */
	private void refreshUnits() {
		ItemStack crown = this.effectiveCrown();
		if (!ItemStack.isSameItem(crown, this.lastCrown) || crown.isEmpty() != this.lastCrown.isEmpty()) {
			this.lastCrown = crown.copy();
			this.units = crown.isEmpty() ? List.of() : ModUnits.forCrown(crown.getItem());
			// A crown with only one kind of unit picks it straight away.
			this.selectedUnit.set(this.units.size() == 1 ? 0 : -1);
		}
	}

	private boolean isPaid(UnitType unit) {
		ItemStack a = this.costASlot.getItem();
		ItemStack b = this.costBSlot.getItem();
		return unit.costA().isPaidBy(a) && unit.costB().isPaidBy(b)
				|| unit.costA().isPaidBy(b) && unit.costB().isPaidBy(a);
	}

	private void updateResult() {
		Optional<UnitType> unit = this.selectedUnit();
		this.resultSlot.set(unit.isPresent() && this.isPaid(unit.get()) ? new ItemStack(unit.get().item()) : ItemStack.EMPTY);
		this.broadcastChanges();
	}

	/** Uses up the chosen unit's cost from the cost slots, matching each cost to the slot that pays it. */
	private void payForUnit() {
		this.selectedUnit().ifPresent(unit -> {
			boolean straight = unit.costA().isPaidBy(this.costASlot.getItem()) && unit.costB().isPaidBy(this.costBSlot.getItem());
			this.costASlot.remove(straight ? unit.costA().count() : unit.costB().count());
			this.costBSlot.remove(straight ? unit.costB().count() : unit.costA().count());
		});
		this.updateResult();
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, ModBlocks.COMMANDER_TABLE);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != this.result && super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex == RESULT_SLOT) {
			// Shift-click makes as many units as the cost items allow.
			stack.getItem().onCraftedBy(stack, player);
			if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, original);
		} else if (slotIndex < INV_SLOT_START) {
			if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (ModItems.isCrown(stack.getItem()) && !this.crownSlot.hasItem()) {
			if (!this.moveItemStackTo(stack, CROWN_SLOT, CROWN_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, COST_A_SLOT, COST_B_SLOT + 1, false)) {
			if (slotIndex < INV_SLOT_END) {
				if (!this.moveItemStackTo(stack, INV_SLOT_END, HOTBAR_SLOT_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.result.removeItemNoUpdate(0);
		this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
	}
}

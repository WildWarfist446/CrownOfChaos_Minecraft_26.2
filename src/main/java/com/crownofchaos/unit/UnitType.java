package com.crownofchaos.unit;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A kind of army unit: the crown that commands it, the mob it becomes, its unit item, and the
 * two items it costs to make at a Commander's Table.
 *
 * @param id     the unit's name, also used for its item ({@code <id>_unit})
 * @param crown  the (base) crown whose wearer can make and command it
 * @param entity the mob it will spawn
 * @param item   the unit item made at the table
 * @param costA  the first item it costs
 * @param costB  the second item it costs
 */
public record UnitType(String id, Item crown, EntityType<?> entity, Item item, Cost costA, Cost costB) {
	/** An item and how many of it. */
	public record Cost(Item item, int count) {
		public boolean isPaidBy(ItemStack stack) {
			return stack.is(this.item) && stack.getCount() >= this.count;
		}

		public ItemStack toStack() {
			return new ItemStack(this.item, this.count);
		}
	}
}

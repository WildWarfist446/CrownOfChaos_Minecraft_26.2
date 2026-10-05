package com.crownofchaos.unit;

import java.util.Optional;

import net.minecraft.tags.TagKey;
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
	/**
	 * An item (or any item with a tag, such as any wool) and how many of it.
	 *
	 * @param item  the item, or for a tag the item the table shows as a hint
	 * @param tag   if present, any item with this tag pays the cost
	 * @param count how many
	 */
	public record Cost(Item item, Optional<TagKey<Item>> tag, int count) {
		public static Cost of(Item item, int count) {
			return new Cost(item, Optional.empty(), count);
		}

		/** Any item with the tag; {@code shown} is the one the table shows as a hint. */
		public static Cost anyOf(TagKey<Item> tag, Item shown, int count) {
			return new Cost(shown, Optional.of(tag), count);
		}

		public boolean isPaidBy(ItemStack stack) {
			boolean right = this.tag.map(stack::is).orElseGet(() -> stack.is(this.item));
			return right && stack.getCount() >= this.count;
		}

		public ItemStack toStack() {
			return new ItemStack(this.item, this.count);
		}
	}
}

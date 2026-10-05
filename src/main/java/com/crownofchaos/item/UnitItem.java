package com.crownofchaos.item;

import java.util.function.Consumer;

import com.crownofchaos.unit.ModUnits;
import com.crownofchaos.unit.UnitType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * An army unit item, made at a Commander's Table. It looks like the mob's spawn egg. Placing
 * units as mobs comes in a later part of the faction system.
 */
public final class UnitItem extends Item {
	public UnitItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		UnitType unit = ModUnits.byItem(this);
		if (unit != null) {
			builder.accept(Component.translatable("item.crownofchaos.unit.commanded_by", Component.translatable(unit.crown().getDescriptionId())).withStyle(ChatFormatting.GRAY));
		}
		builder.accept(Component.translatable("item.crownofchaos.unit.coming_soon").withStyle(ChatFormatting.DARK_GRAY));
	}
}

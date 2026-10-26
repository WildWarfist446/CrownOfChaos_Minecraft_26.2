package com.crownofchaos.item;

import com.crownofchaos.lore.LoreBook;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** "A Warning from a Former King": using it opens the lore book (see {@link LoreBook}). */
public final class LoreBookItem extends Item {
	public LoreBookItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			LoreBook.open(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}
}

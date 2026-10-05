package com.crownofchaos.item;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.crownofchaos.faction.Faction;
import com.crownofchaos.faction.FactionData;
import com.crownofchaos.unit.ModUnits;
import com.crownofchaos.unit.UnitManager;
import com.crownofchaos.unit.UnitType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * An army unit item, made at a Commander's Table. It looks like the mob's spawn egg; using it
 * on a block summons the mob as a unit of the player's faction, tinted in the faction's color.
 * Units are passive unless the faction rules say to fight (see {@link UnitManager}).
 */
public final class UnitItem extends Item {
	public UnitItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		UnitType unit = ModUnits.byItem(this);
		if (unit == null || !unit.entity().canSpawn(context.getLevel())) {
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		Optional<Faction> faction = FactionData.get(level.getServer()).factionOf(player.getUUID());
		if (faction.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.unit.no_faction").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		// Where to put it, as a spawn egg does: in the clicked block if it's open, otherwise beside it.
		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
		@SuppressWarnings("unchecked")
		EntityType<Entity> type = (EntityType<Entity>) unit.entity();
		Entity spawned = type.spawn(level, entity -> {
			if (entity instanceof Mob mob) {
				UnitManager.makeUnit(mob, player, faction.get(), unit);
			}
		}, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE, true, !Objects.equals(clicked, spawnPos) && face == Direction.UP);
		if (spawned == null) {
			return InteractionResult.FAIL;
		}
		context.getItemInHand().consume(1, player);
		level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		UnitType unit = ModUnits.byItem(this);
		if (unit != null) {
			builder.accept(Component.translatable("item.crownofchaos.unit.commanded_by", Component.translatable(unit.crown().getDescriptionId())).withStyle(ChatFormatting.GRAY));
		}
		builder.accept(Component.translatable("item.crownofchaos.unit.how_to_use").withStyle(ChatFormatting.DARK_GRAY));
	}
}

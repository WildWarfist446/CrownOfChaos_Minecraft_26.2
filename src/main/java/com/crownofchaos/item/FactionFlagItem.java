package com.crownofchaos.item;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.faction.Faction;
import com.crownofchaos.faction.FactionData;
import com.crownofchaos.faction.FactionFlag;
import com.crownofchaos.faction.FactionManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Blocks;

/**
 * A Faction Flag. Crafting one makes it belong to the crafter's faction (a flag from the
 * creative menu belongs to whoever first places it). Placing it puts down a banner in the
 * faction's color and claims everything within {@link FactionManager#CLAIM_RADIUS} blocks for
 * that faction. Only members can place it, and not so close to another faction's flag that
 * their territories would overlap. Breaking the banner removes the claim and drops the flag.
 */
public final class FactionFlagItem extends BannerItem {
	/**
	 * It is a banner item (on a plain white banner's blocks, which it never places: it places a banner
	 * of its faction's color itself), so a loom takes it, patterns and dyes can be added, and a cauldron
	 * washes them off, as with any banner. The patterns are kept when it is placed and when it is picked up.
	 */
	public FactionFlagItem(Item.Properties properties) {
		super(Blocks.BANNER.pick(DyeColor.WHITE), Blocks.WALL_BANNER.pick(DyeColor.WHITE), properties);
	}

	/** A Faction Flag belonging to the faction. */
	public static ItemStack forFaction(Faction faction) {
		ItemStack stack = new ItemStack(ModItems.FACTION_FLAG);
		assign(stack, faction);
		return stack;
	}

	private static void assign(ItemStack stack, Faction faction) {
		stack.set(ModComponents.FLAG_FACTION, new ModComponents.FlagFaction(faction.id(), faction.name()));
		stack.set(ModComponents.FLAG_COLOR, faction.color());
	}

	@Override
	public void onCraftedBy(ItemStack stack, Player player) {
		super.onCraftedBy(stack, player);
		if (player instanceof ServerPlayer serverPlayer) {
			FactionData.get(serverPlayer.level().getServer()).factionOf(player.getUUID()).ifPresent(faction -> assign(stack, faction));
		}
	}

	/** Keeps the flag's color and name in step with its faction, if the faction is renamed or recolored. */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		ModComponents.FlagFaction belongsTo = stack.get(ModComponents.FLAG_FACTION);
		if (belongsTo == null) {
			return;
		}
		Faction faction = FactionData.get(level.getServer()).get(belongsTo.id());
		if (faction != null && (faction.color() != stack.get(ModComponents.FLAG_COLOR) || !faction.name().equals(belongsTo.name()))) {
			assign(stack, faction);
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			// The server decides; the client just swings.
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = context.getItemInHand();
		FactionData data = FactionData.get(player.level().getServer());
		Optional<Faction> mine = data.factionOf(player.getUUID());
		if (mine.isEmpty()) {
			return fail(player, Component.translatable("message.crownofchaos.faction.flag_no_faction"));
		}
		Faction faction = mine.get();
		ModComponents.FlagFaction belongsTo = stack.get(ModComponents.FLAG_FACTION);
		if (belongsTo != null && !belongsTo.id().equals(faction.id())) {
			Faction other = data.get(belongsTo.id());
			return fail(player, Component.translatable("message.crownofchaos.faction.flag_not_yours",
					other != null ? other.displayName() : Component.literal(belongsTo.name())));
		}

		BlockPlaceContext placeContext = new BlockPlaceContext(context);
		BlockPos pos = placeContext.getClickedPos();
		Optional<Faction> overlapping = data.overlappingFaction(player.level().dimension(), pos.getX() + 0.5, pos.getZ() + 0.5, faction.id());
		if (overlapping.isPresent()) {
			return fail(player, Component.translatable("message.crownofchaos.faction.flag_too_close",
					overlapping.get().displayName(), FactionManager.CLAIM_RADIUS * 2));
		}

		// Place it like a banner of the faction's color, then claim the land around it.
		assign(stack, faction);
		InteractionResult result = bannerItem(faction.color()).place(placeContext);
		if (result.consumesAction() && player.level().getBlockState(pos).getBlock() instanceof AbstractBannerBlock) {
			FactionManager.addFlag(player, faction, new FactionFlag(player.level().dimension(), pos.immutable()));
		}
		return result;
	}

	private static InteractionResult fail(ServerPlayer player, Component message) {
		player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.RED));
		return InteractionResult.FAIL;
	}

	private static BlockItem bannerItem(DyeColor color) {
		return (BlockItem) BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + "_banner"));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		ModComponents.FlagFaction belongsTo = stack.get(ModComponents.FLAG_FACTION);
		DyeColor color = stack.getOrDefault(ModComponents.FLAG_COLOR, DyeColor.WHITE);
		if (belongsTo != null) {
			builder.accept(Component.translatable("item.crownofchaos.faction_flag.faction",
					Component.literal(belongsTo.name()).withColor(color.getTextColor())).withStyle(ChatFormatting.GRAY));
		} else {
			builder.accept(Component.translatable("item.crownofchaos.faction_flag.unassigned").withStyle(ChatFormatting.GRAY));
		}
		builder.accept(Component.translatable("item.crownofchaos.faction_flag.claims", FactionManager.CLAIM_RADIUS).withStyle(ChatFormatting.GRAY));
	}
}

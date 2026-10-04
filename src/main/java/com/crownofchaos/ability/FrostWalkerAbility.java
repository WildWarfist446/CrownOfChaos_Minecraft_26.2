package com.crownofchaos.ability;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Crown of Winter ability: pressing the ability key toggles Frost Walker II on or off. While
 * it's on, it works exactly like Frost Walker II boots: still water under the wearer freezes
 * into frosted ice in a radius of 4, and magma blocks and campfires don't burn them.
 */
public final class FrostWalkerAbility implements CrownAbility {
	/** Frost Walker's radius is 3 + 1 per level above I, so 4 at level II. */
	private static final int RADIUS = 4;

	/** Players who have Frost Walker switched on. Resets when they leave the server. */
	private static final Set<UUID> ENABLED = new HashSet<>();

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		boolean enabled = ENABLED.add(player.getUUID());
		if (!enabled) {
			ENABLED.remove(player.getUUID());
		}

		player.sendOverlayMessage(enabled
				? Component.translatable("message.crownofchaos.ability.frost_walker.enabled").withStyle(ChatFormatting.GREEN)
				: Component.translatable("message.crownofchaos.ability.frost_walker.disabled").withStyle(ChatFormatting.RED));
		// Toggling isn't a use of the ability, so no cooldown.
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private static boolean isActive(ServerPlayer player) {
		return ENABLED.contains(player.getUUID()) && CrownEffects.isWearing(player, ModItems.CROWN_OF_WINTER);
	}

	static void initialize() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ENABLED.remove(handler.getPlayer().getUUID()));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// Like the enchantment, only while standing on something and not riding.
				if (player.onGround() && !player.isPassenger() && isActive(player)) {
					freezeWater((ServerLevel) player.level(), player);
				}
			}
		});

		// Like the enchantment, no burning from magma blocks or campfires.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer player
						&& source.is(DamageTypeTags.BURN_FROM_STEPPING)
						&& !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
						&& isActive(player)));
	}

	/** The same disk of frosted ice the Frost Walker enchantment places (see vanilla {@code ReplaceDisk}). */
	private static void freezeWater(ServerLevel level, ServerPlayer player) {
		Vec3 position = player.position();
		BlockPos center = BlockPos.containing(position).below();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, 0, -RADIUS), center.offset(RADIUS, 0, RADIUS))) {
			if (pos.distToCenterSqr(position.x(), pos.getY() + 0.5, position.z()) < Mth.square(RADIUS)
					&& level.getBlockState(pos.above()).is(BlockTags.AIR)
					&& level.getBlockState(pos).is(Blocks.WATER)
					&& level.getFluidState(pos).isSourceOfType(Fluids.WATER)
					&& level.isUnobstructed(null, Shapes.block().move(pos))
					&& level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState())) {
				level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
			}
		}
	}
}

package com.crownofchaos.ability;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Under Crown ability: pressing the ability key toggles Vein Miner on or off. While it is on,
 * breaking an ore also breaks every connected ore of the same kind. Each extra block is broken as if the player mined it
 * themselves, so the tool loses durability, Fortune applies and XP drops as normal.
 */
public final class VeinMinerAbility implements CrownAbility {
	/** Most extra blocks a single vein mine can break, on top of the one the player mined. */
	private static final int MAX_BLOCKS = 64;

	/**
	 * Vanilla ore families. Ores in the same family count as one vein, so stone and
	 * deepslate variants mix; any other ore only connects to the exact same block.
	 */
	private static final List<TagKey<Block>> ORE_FAMILIES = List.of(
			BlockItemTags.COAL_ORES.block(),
			BlockItemTags.COPPER_ORES.block(),
			BlockItemTags.IRON_ORES.block(),
			BlockItemTags.GOLD_ORES.block(),
			BlockItemTags.REDSTONE_ORES.block(),
			BlockItemTags.LAPIS_ORES.block(),
			BlockItemTags.DIAMOND_ORES.block(),
			BlockItemTags.EMERALD_ORES.block());

	/** Players whose vein mine is in progress, so the blocks it breaks don't start another. */
	private static final Set<UUID> MINING = new HashSet<>();

	private static final ToggleState TOGGLE = new ToggleState("vein_miner");

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		TOGGLE.toggle(player);
		// Toggling isn't a use of the ability, so no cooldown.
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	static void initialize() {
		PlayerBlockBreakEvents.AFTER.register(VeinMinerAbility::afterBlockBreak);
	}

	private static void afterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(player instanceof ServerPlayer serverPlayer)
				|| MINING.contains(player.getUUID())
				|| !state.is(ConventionalBlockTags.ORES)
				|| !CrownEffects.isWearing(serverPlayer, ModItems.UNDER_CROWN)
				|| !TOGGLE.isEnabled(player)
				|| !serverPlayer.hasCorrectToolForDrops(state)) {
			return;
		}

		MINING.add(player.getUUID());
		try {
			mineVein(level, serverPlayer, pos, sameVein(state));
		} finally {
			MINING.remove(player.getUUID());
		}
	}

	private static void mineVein(Level level, ServerPlayer player, BlockPos origin, Predicate<BlockState> sameVein) {
		ItemStack tool = player.getMainHandItem();
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		visited.add(origin);
		queueNeighbors(level, origin, sameVein, visited, queue);

		int mined = 0;
		while (!queue.isEmpty() && mined < MAX_BLOCKS) {
			// Stop if the tool was swapped, or before the hit that would break it.
			if (player.getMainHandItem() != tool || tool.isDamageableItem() && tool.nextDamageWillBreak()) {
				return;
			}

			BlockPos pos = queue.poll();
			BlockState state = level.getBlockState(pos);
			if (!sameVein.test(state) || !player.hasCorrectToolForDrops(state)) {
				continue;
			}

			// The normal player mining path: tool durability, Fortune, XP, hunger and stats all
			// apply, and protection mods can still refuse the break.
			if (player.gameMode.destroyBlock(pos)) {
				mined++;
				queueNeighbors(level, pos, sameVein, visited, queue);
			}
		}
	}

	/** Queues every matching block touching {@code pos}, including diagonals. */
	private static void queueNeighbors(Level level, BlockPos pos, Predicate<BlockState> sameVein, Set<BlockPos> visited, Queue<BlockPos> queue) {
		for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (!visited.contains(neighbor) && sameVein.test(level.getBlockState(neighbor))) {
				BlockPos immutable = neighbor.immutable();
				visited.add(immutable);
				queue.add(immutable);
			}
		}
	}

	private static Predicate<BlockState> sameVein(BlockState origin) {
		for (TagKey<Block> family : ORE_FAMILIES) {
			if (origin.is(family)) {
				return state -> state.is(family);
			}
		}
		Block block = origin.getBlock();
		return state -> state.is(block);
	}
}

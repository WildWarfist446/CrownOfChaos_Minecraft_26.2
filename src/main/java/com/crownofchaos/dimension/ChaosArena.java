package com.crownofchaos.dimension;

import java.util.List;
import java.util.Set;

import com.crownofchaos.CrownOfChaos;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Chaos Arena: the boss dimension ({@code crownofchaos:chaos_arena}, defined in data). It is a
 * void with one round, floating arena: a floor that is a glitched mix of netherrack, cobblestone,
 * mossy cobblestone and nether bricks over a layer of bedrock, with the "Herobrine altar" in the
 * middle (a 3x3 of gold blocks standing on the floor, a netherrack on top of the centre, and four redstone torches around
 * it). Lighting the netherrack is meant to start the boss fight; the boss is not built yet, so for
 * now lighting it only announces itself.
 *
 * <p>The dimension itself is empty; the arena is built into it when the server starts, and again
 * whenever it is missing, see {@link #ensureBuilt}.
 */
public final class ChaosArena {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, CrownOfChaos.id("chaos_arena"));

	/** The arena floor is a disc this many blocks from the centre to the edge (about 37 across). */
	public static final int ARENA_RADIUS = 18;
	/** The y of the top layer of the floor; the bedrock layer is just under it. */
	public static final int FLOOR_Y = 64;
	/** The altar's centre block, the netherrack you light: on top of the gold blocks, which stand on the floor. */
	public static final BlockPos ALTAR = new BlockPos(0, FLOOR_Y + 2, 0);
	/** Where players arrive: near the edge, south of the altar, facing it. */
	public static final BlockPos ARRIVAL = new BlockPos(0, FLOOR_Y + 1, -(ARENA_RADIUS - 4));

	/** The floor's blocks, mixed at random. */
	private static final List<BlockState> FLOOR_BLOCKS = List.of(
			Blocks.NETHERRACK.defaultBlockState(),
			Blocks.COBBLESTONE.defaultBlockState(),
			Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
			Blocks.NETHER_BRICKS.defaultBlockState());

	/** How often (in ticks) the altar is checked for fire. */
	private static final int ALTAR_CHECK_TICKS = 10;

	private static boolean altarWasLit;

	private ChaosArena() {
	}

	public static void initialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel level = server.getLevel(DIMENSION);
			if (level != null) {
				ensureBuilt(level);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> altarWasLit = false);
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.dimension() == DIMENSION && level.getServer().getTickCount() % ALTAR_CHECK_TICKS == 0) {
				checkAltar(level);
			}
		});
	}

	/** Sends the player to the arena's arrival spot, building the arena first if needed. */
	public static boolean enter(ServerPlayer player) {
		ServerLevel level = player.level().getServer().getLevel(DIMENSION);
		if (level == null) {
			return false;
		}
		ensureBuilt(level);
		// Facing the altar: it is to the north-south axis' other side, so yaw 0 (facing +z).
		return player.teleportTo(level, ARRIVAL.getX() + 0.5, ARRIVAL.getY(), ARRIVAL.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, true);
	}

	/** Builds the arena if its altar is not there. Safe to call any time. */
	public static void ensureBuilt(ServerLevel level) {
		if (level.getBlockState(ALTAR.below()).is(Blocks.GOLD_BLOCK)) {
			return;
		}
		CrownOfChaos.LOGGER.info("Building the Chaos Arena");

		int radiusSquared = ARENA_RADIUS * ARENA_RADIUS;
		BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
		for (int x = -ARENA_RADIUS; x <= ARENA_RADIUS; x++) {
			for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++) {
				if (x * x + z * z > radiusSquared) {
					continue;
				}
				// The same glitched mix every time for a given spot, so rebuilding does not change it.
				RandomSource random = RandomSource.create(Mth.getSeed(x, FLOOR_Y, z));
				place(level, new BlockPos(x, FLOOR_Y, z), FLOOR_BLOCKS.get(random.nextInt(FLOOR_BLOCKS.size())));
				place(level, new BlockPos(x, FLOOR_Y - 1, z), bedrock);
			}
		}

		// The altar: 3x3 gold blocks standing on the floor (so their sides show), the netherrack on the
		// centre one, and a redstone torch on each gold block beside it.
		BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				place(level, new BlockPos(x, FLOOR_Y + 1, z), gold);
			}
		}
		place(level, ALTAR, Blocks.NETHERRACK.defaultBlockState());
		BlockState torch = Blocks.REDSTONE_TORCH.defaultBlockState();
		place(level, ALTAR.north(), torch);
		place(level, ALTAR.south(), torch);
		place(level, ALTAR.east(), torch);
		place(level, ALTAR.west(), torch);
	}

	private static void place(ServerLevel level, BlockPos pos, BlockState state) {
		// No neighbor updates: nothing in the arena needs them while it is being built.
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
	}

	/** Detects the altar being lit (fire appearing on top of its netherrack). */
	private static void checkAltar(ServerLevel level) {
		boolean lit = level.getBlockState(ALTAR.above()).is(Blocks.FIRE);
		if (lit && !altarWasLit) {
			onAltarLit(level);
		}
		altarWasLit = lit;
	}

	/** The fight starts here. For now it only announces itself: the boss comes next. */
	private static void onAltarLit(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(Component.literal("The altar burns...").withStyle(ChatFormatting.DARK_PURPLE));
		}
		level.playSound(null, ALTAR, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.6F);
	}
}

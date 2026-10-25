package com.crownofchaos.dimension;

import java.util.List;
import java.util.Set;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.block.ModBlocks;
import com.crownofchaos.network.BossMusicPayload;
import com.crownofchaos.entity.ChaosKing;
import com.crownofchaos.entity.ModEntities;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Chaos Arena: the boss dimension ({@code crownofchaos:chaos_arena}, defined in data). It is a
 * void with one round, floating arena: a floor that is a glitched mix of netherrack, cobblestone,
 * mossy cobblestone and nether bricks over a layer of bedrock, with the "Herobrine altar" in the
 * middle (a 3x3 of gold blocks standing on the floor, a netherrack on top of the centre, and four redstone torches around
 * it), all inside a solid wall of lava. Lighting the netherrack is meant to start the boss fight; the boss is not built yet, so for
 * now lighting it only announces itself.
 *
 * <p>The dimension itself is empty; the arena is built into it when the server starts, and again
 * whenever it is missing, see {@link #ensureBuilt}.
 */
public final class ChaosArena {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, CrownOfChaos.id("chaos_arena"));

	/** The arena floor is a disc this many blocks from the centre to the edge (about 73 across: twice the first version, so several players and their units fit). */
	public static final int ARENA_RADIUS = 36;
	/** Radii the arena has had before (the first version, and a briefly tried triple size), cleared away from worlds that still have them. */
	private static final int[] LEGACY_ARENA_RADII = {18, 54};
	/** The y of the top layer of the floor; the bedrock layer is just under it. */
	public static final int FLOOR_Y = 64;
	/** The altar's centre block, the netherrack you light: on top of the gold blocks, which stand on the floor. */
	public static final BlockPos ALTAR = new BlockPos(0, FLOOR_Y + 2, 0);
	/** Where players arrive: near the edge, south of the altar, facing it. */
	public static final BlockPos ARRIVAL = new BlockPos(0, FLOOR_Y + 1, -(ARENA_RADIUS - 4));

	/** The lava wall around the arena is this many blocks thick, starting right at the floor's edge. */
	public static final int LAVA_WALL_THICKNESS = 4;
	/** The lava wall runs from this many blocks below the floor... */
	public static final int LAVA_WALL_BELOW_FLOOR = 6;
	/** ...to this many blocks above it. */
	public static final int LAVA_WALL_ABOVE_FLOOR = 100;
	/** A block of the floor's bedrock layer at its edge, which tells whether the floor has its current size. */
	private static final BlockPos FLOOR_MARKER = new BlockPos(ARENA_RADIUS - 1, FLOOR_Y - 1, 0);

	/** The lava wall is sealed in bedrock: a ring outside it, a floor under it and a lid over the whole arena. */
	private static final int BEDROCK_SHELL_THICKNESS = 1;
	/** A block of the shell (in its outer ring) that tells whether it has been built. */
	private static final BlockPos BEDROCK_SHELL_MARKER = new BlockPos(ARENA_RADIUS + LAVA_WALL_THICKNESS + 1, FLOOR_Y, 0);

	/** A block at the very top of the lava wall that tells whether it has been built (so a lower, older wall is rebuilt taller). */
	private static final BlockPos LAVA_WALL_MARKER = new BlockPos(ARENA_RADIUS + 2, FLOOR_Y + LAVA_WALL_ABOVE_FLOOR, 0);

	/** Where the Chaos King appears when the altar is lit: in the fire on top of its netherrack. */
	public static final BlockPos BOSS_SPAWN = ALTAR.above();
	/** Where he lands once his legion is dead: on top of the altar, where he appeared. */
	public static final BlockPos BOSS_LANDING = ALTAR.above();

	/** The floor's blocks, mixed at random. */
	private static final List<BlockState> FLOOR_BLOCKS = List.of(
			Blocks.NETHERRACK.defaultBlockState(),
			Blocks.COBBLESTONE.defaultBlockState(),
			Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
			Blocks.NETHER_BRICKS.defaultBlockState());

	/** How often (in ticks) the altar is checked for fire. */
	private static final int ALTAR_CHECK_TICKS = 10;

	private static boolean altarWasLit;

	/** How often (in ticks) the fight music is started for new arrivals and stopped when the fight ends. */
	private static final int MUSIC_CHECK_TICKS = 20;
	/** The players whose game is playing the fight music. */
	private static final java.util.Set<java.util.UUID> musicPlayers = new java.util.HashSet<>();

	/**
	 * Plays the fight music to everyone in the arena while the Chaos King is alive, and stops it for
	 * anyone it was playing for once he is gone (beaten, or the fight reset) or they left. The client
	 * loops the track itself and also stops if its player leaves the dimension.
	 */
	private static void updateMusic(ServerLevel level) {
		boolean fighting = !level.getEntities(ModEntities.CHAOS_KING, king -> king.isAlive()).isEmpty();
		java.util.Set<java.util.UUID> wanted = new java.util.HashSet<>();
		if (fighting) {
			for (ServerPlayer player : level.players()) {
				wanted.add(player.getUUID());
				if (musicPlayers.add(player.getUUID()) && ServerPlayNetworking.canSend(player, BossMusicPayload.TYPE)) {
					ServerPlayNetworking.send(player, new BossMusicPayload(true));
				}
			}
		}
		for (java.util.Iterator<java.util.UUID> it = musicPlayers.iterator(); it.hasNext();) {
			java.util.UUID id = it.next();
			if (!wanted.contains(id)) {
				it.remove();
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
				if (player != null && ServerPlayNetworking.canSend(player, BossMusicPayload.TYPE)) {
					ServerPlayNetworking.send(player, new BossMusicPayload(false));
				}
			}
		}
	}

	private ChaosArena() {
	}

	public static void initialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel level = server.getLevel(DIMENSION);
			if (level != null) {
				ensureBuilt(level);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			altarWasLit = false;
			musicPlayers.clear();
		});
		PayloadTypeRegistry.clientboundPlay().register(BossMusicPayload.TYPE, BossMusicPayload.CODEC);
		// When the last player dies or logs out, the fight resets at once; the boss's own tick (every second) covers leaving the dimension.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
				// He laughs when a player falls, before a wipe (if it was the last) clears him away.
				if (level.dimension() == DIMENSION) {
					level.getEntitiesOfClass(ChaosKing.class, arenaBox()).forEach(king -> king.laugh(level));
				}
				resetIfEmpty(level, player);
			}
		});
		// ...and when one of his abilities (a skull, a fireball or a lightning strike) hurts someone; his own legion doesn't count.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0.0F && source.getEntity() instanceof ChaosKing king && source.getDirectEntity() != king
					&& !ChaosKing.isLegion(target) && target.level() instanceof ServerLevel level) {
				king.laugh(level);
			}
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerLevel level = server.getLevel(DIMENSION);
			if (level != null) {
				resetIfEmpty(level, handler.getPlayer());
			}
		});
		// The Chaos King's legion does not hurt itself: no arrows, fangs, beams, blasts or bites between its own.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.getEntity() != null && source.getEntity() != entity && ChaosKing.isLegion(entity) && ChaosKing.isLegion(source.getEntity())));
		// Vexes called up by his Evokers join his side.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Vex vex && vex.getOwner() != null && vex.getOwner().entityTags().contains(ChaosKing.MINION_TAG)) {
				vex.addTag(ChaosKing.ALLY_TAG);
			}
		});
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.dimension() != DIMENSION) {
				return;
			}
			checkSoulFire(level);
			if (level.getServer().getTickCount() % MUSIC_CHECK_TICKS == 0) {
				updateMusic(level);
			}
			if (level.getServer().getTickCount() % ALTAR_CHECK_TICKS == 0) {
				checkAltar(level);
			}
		});
	}

	private static AABB arenaBox() {
		return new AABB(-ARENA_RADIUS - 1, FLOOR_Y - 10, -ARENA_RADIUS - 1, ARENA_RADIUS + 1, FLOOR_Y + 120, ARENA_RADIUS + 1);
	}

	/** The living players in the arena (not spectators). The fight is over when there are none. */
	public static java.util.List<ServerPlayer> livingPlayers(ServerLevel level) {
		AABB box = new AABB(-ARENA_RADIUS - 1, FLOOR_Y - 10, -ARENA_RADIUS - 1, ARENA_RADIUS + 1, FLOOR_Y + 120, ARENA_RADIUS + 1);
		return level.players().stream().filter(p -> p.isAlive() && !p.isSpectator() && box.contains(p.position())).toList();
	}

	/**
	 * Ends the fight: the Chaos King, his legion and the altar's fire are cleared, so the altar has to be
	 * lit again to start over. Done when every player in the arena is dead or has left. A soul fire from a
	 * won fight is left alone.
	 */
	public static void resetFight(ServerLevel level) {
		AABB everything = new AABB(-ARENA_RADIUS - 2, FLOOR_Y - 10, -ARENA_RADIUS - 2, ARENA_RADIUS + 2, FLOOR_Y + 130, ARENA_RADIUS + 2);
		boolean any = false;
		for (net.minecraft.world.entity.Entity entity : level.getEntities((net.minecraft.world.entity.Entity) null, everything, ChaosKing::isLegion)) {
			entity.discard();
			any = true;
		}
		if (level.getBlockState(ALTAR.above()).is(Blocks.FIRE)) {
			level.setBlock(ALTAR.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			any = true;
		}
		altarWasLit = false;
		if (any) {
			CrownOfChaos.LOGGER.info("The Chaos Arena fight was reset: no players left");
		}
	}

	/** Resets the fight if no living player is left in the arena (skipping one who is leaving). */
	private static void resetIfEmpty(ServerLevel level, @org.jspecify.annotations.Nullable ServerPlayer leaving) {
		if (level.dimension() == DIMENSION && livingPlayers(level).stream().allMatch(p -> p == leaving)) {
			resetFight(level);
		}
	}

	/** The bottom corner of the exit portal's 2 wide, 3 tall opening, behind the arrival spot. A solid bedrock row sits right behind it. */
	public static final BlockPos EXIT_PORTAL = new BlockPos(0, FLOOR_Y + 1, -(ARENA_RADIUS - 2));

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

	/** Builds the arena, and its lava wall, if they are not there. Safe to call any time. */
	public static void ensureBuilt(ServerLevel level) {
		boolean altarBuilt = level.getBlockState(ALTAR.below()).is(Blocks.GOLD_BLOCK);
		for (int legacy : LEGACY_ARENA_RADII) {
			// An arena of another size (its lava wall starts right at its edge): clear what doesn't fit the current one.
			if (legacy != ARENA_RADIUS && level.getBlockState(new BlockPos(legacy + 1, FLOOR_Y + 1, 0)).is(Blocks.LAVA)) {
				clearLegacyArena(level, legacy);
			}
		}
		if (!level.getBlockState(FLOOR_MARKER).is(Blocks.BEDROCK)) {
			buildFloor(level);
		}
		if (!altarBuilt) {
			buildAltar(level);
		}
		if (!level.getBlockState(LAVA_WALL_MARKER).is(Blocks.LAVA)) {
			buildLavaWall(level);
		}
		if (!level.getBlockState(BEDROCK_SHELL_MARKER).is(Blocks.BEDROCK)) {
			buildBedrockShell(level);
		}
		// The way out exists only once the boss is beaten (the soul fire on the altar marks that).
		// (An earlier version put it one block further back, so that spot is checked too.)
		boolean beaten = level.getBlockState(ALTAR.above()).is(Blocks.SOUL_FIRE);
		boolean hasExit = level.getBlockState(EXIT_PORTAL).is(ModBlocks.NULL_EXIT);
		boolean hasOldExit = level.getBlockState(EXIT_PORTAL.north()).is(ModBlocks.NULL_EXIT);
		if (!beaten && (hasExit || hasOldExit)) {
			removeExitPortal(level);
		} else if (beaten && (!hasExit || hasOldExit)) {
			buildExitPortal(level);
		}
	}

	/** The glitched floor block for a spot: the same mix every time, so rebuilding does not change it. */
	private static BlockState floorBlockAt(int x, int z) {
		RandomSource random = RandomSource.create(Mth.getSeed(x, FLOOR_Y, z));
		return FLOOR_BLOCKS.get(random.nextInt(FLOOR_BLOCKS.size()));
	}

	/** The way back: a bedrock frame filled with unbreakable null blocks, behind the arrival spot. */
	private static void removeExitPortal(ServerLevel level) {
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = EXIT_PORTAL.getX() - 1; x <= EXIT_PORTAL.getX() + 2; x++) {
			for (int z = EXIT_PORTAL.getZ() - 1; z <= EXIT_PORTAL.getZ(); z++) {
				// The frame's bottom row stood in the floor: put the floor back.
				place(level, new BlockPos(x, FLOOR_Y, z), floorBlockAt(x, z));
				for (int y = EXIT_PORTAL.getY(); y <= EXIT_PORTAL.getY() + 3; y++) {
					place(level, new BlockPos(x, y, z), air);
				}
			}
		}
	}

	private static void buildExitPortal(ServerLevel level) {
		BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
		BlockState exit = ModBlocks.NULL_EXIT.defaultBlockState();
		for (int x = EXIT_PORTAL.getX() - 1; x <= EXIT_PORTAL.getX() + 2; x++) {
			for (int y = EXIT_PORTAL.getY() - 1; y <= EXIT_PORTAL.getY() + 3; y++) {
				boolean inside = x >= EXIT_PORTAL.getX() && x <= EXIT_PORTAL.getX() + 1 && y >= EXIT_PORTAL.getY() && y <= EXIT_PORTAL.getY() + 2;
				place(level, new BlockPos(x, y, EXIT_PORTAL.getZ()), inside ? exit : bedrock);
				// A solid row right behind it, so walking through the portal can't lead on towards the lava.
				if (y >= EXIT_PORTAL.getY()) {
					place(level, new BlockPos(x, y, EXIT_PORTAL.getZ() - 1), bedrock);
				}
				// ...and the floor under that row, which an earlier exit position left a hole in.
				if (y == EXIT_PORTAL.getY() - 1 && level.getBlockState(new BlockPos(x, y, EXIT_PORTAL.getZ() - 1)).isAir()) {
					place(level, new BlockPos(x, y, EXIT_PORTAL.getZ() - 1), floorBlockAt(x, EXIT_PORTAL.getZ() - 1));
				}
			}
		}
	}

	/**
	 * A solid wall of lava all the way round the arena, right up against the floor's edge. The lava
	 * never flows in this dimension (see {@link com.crownofchaos.mixin.FluidSpreadMixin}), so it stays a
	 * wall and does not pour onto the arena or into the void.
	 */
	private static void buildLavaWall(ServerLevel level) {
		CrownOfChaos.LOGGER.info("Building the Chaos Arena's lava wall");
		int inner = ARENA_RADIUS * ARENA_RADIUS;
		int outerRadius = ARENA_RADIUS + LAVA_WALL_THICKNESS;
		int outer = outerRadius * outerRadius;
		BlockState lava = Blocks.LAVA.defaultBlockState();
		for (int x = -outerRadius; x <= outerRadius; x++) {
			for (int z = -outerRadius; z <= outerRadius; z++) {
				int distance = x * x + z * z;
				if (distance <= inner || distance > outer) {
					continue;
				}
				for (int y = FLOOR_Y - LAVA_WALL_BELOW_FLOOR; y <= FLOOR_Y + LAVA_WALL_ABOVE_FLOOR; y++) {
					place(level, new BlockPos(x, y, z), lava);
				}
			}
		}
	}

	/**
	 * Bedrock all round the outside of the lava, under it and over the top of the whole arena, so nothing
	 * (the boss included) can get out through the lava or fly out over it.
	 */
	private static void buildBedrockShell(ServerLevel level) {
		CrownOfChaos.LOGGER.info("Building the Chaos Arena's bedrock shell");
		int lavaOuter = ARENA_RADIUS + LAVA_WALL_THICKNESS;
		int shellOuter = lavaOuter + BEDROCK_SHELL_THICKNESS;
		int lavaOuterSquared = lavaOuter * lavaOuter;
		int shellOuterSquared = shellOuter * shellOuter;
		int bottom = FLOOR_Y - LAVA_WALL_BELOW_FLOOR - 1;
		int top = FLOOR_Y + LAVA_WALL_ABOVE_FLOOR + 1;
		BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
		for (int x = -shellOuter; x <= shellOuter; x++) {
			for (int z = -shellOuter; z <= shellOuter; z++) {
				int distance = x * x + z * z;
				if (distance > shellOuterSquared) {
					continue;
				}
				// Under the lava (the arena's own floor is not touched) and over the whole disc.
				if (distance > ARENA_RADIUS * ARENA_RADIUS) {
					place(level, new BlockPos(x, bottom, z), bedrock);
				}
				place(level, new BlockPos(x, top, z), bedrock);
				// The ring just outside the lava.
				if (distance > lavaOuterSquared) {
					for (int y = bottom; y <= top; y++) {
						place(level, new BlockPos(x, y, z), bedrock);
					}
				}
			}
		}
	}

	/**
	 * Clears an arena of another size: its lava wall, bedrock shell and exit portal and, if it was bigger, the
	 * floor and lid that stick out past the current one.
	 */
	private static void clearLegacyArena(ServerLevel level, int legacy) {
		CrownOfChaos.LOGGER.info("Clearing the Chaos Arena's old walls (radius {})", legacy);
		int shellOuter = legacy + LAVA_WALL_THICKNESS + BEDROCK_SHELL_THICKNESS;
		int floorInner = Math.min(legacy, ARENA_RADIUS);
		int top = FLOOR_Y + LAVA_WALL_ABOVE_FLOOR + 1;
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -shellOuter; x <= shellOuter; x++) {
			for (int z = -shellOuter; z <= shellOuter; z++) {
				int distance = x * x + z * z;
				if (distance <= floorInner * floorInner || distance > shellOuter * shellOuter) {
					continue;
				}
				if (distance <= legacy * legacy) {
					// Floor (and its bedrock layer) and lid of a bigger arena, past the new edge.
					place(level, new BlockPos(x, FLOOR_Y - 1, z), air);
					place(level, new BlockPos(x, FLOOR_Y, z), air);
					place(level, new BlockPos(x, top, z), air);
				} else {
					for (int y = FLOOR_Y - LAVA_WALL_BELOW_FLOOR - 1; y <= top; y++) {
						place(level, new BlockPos(x, y, z), air);
					}
				}
			}
		}
		// The old exit portal and its frame, just inside the old edge.
		for (int x = -1; x <= 2; x++) {
			for (int z = -legacy + 1; z <= -legacy + 2; z++) {
				for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
					place(level, new BlockPos(x, y, z), air);
				}
			}
		}
	}

	private static void buildFloor(ServerLevel level) {
		CrownOfChaos.LOGGER.info("Building the Chaos Arena's floor");

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

	}

	private static void buildAltar(ServerLevel level) {
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

	/** The fight starts here: the Chaos King appears in the altar's fire, unless he is already there. */
	private static void onAltarLit(ServerLevel level) {
		// A new fight: the way out closes until he is beaten again.
		removeExitPortal(level);
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(Component.literal("The altar burns...").withStyle(ChatFormatting.DARK_PURPLE));
		}

		AABB arena = new AABB(-ARENA_RADIUS, FLOOR_Y - 2, -ARENA_RADIUS, ARENA_RADIUS, FLOOR_Y + 60, ARENA_RADIUS);
		if (level.getEntitiesOfClass(ChaosKing.class, arena).isEmpty()) {
			ModEntities.CHAOS_KING.spawn(level, BOSS_SPAWN, EntitySpawnReason.TRIGGERED);
		}
	}

	/** The Chaos King is dead: the way out opens, the altar's fire turns to soul fire, which turns a Netherite Upgrade into a Chaos Upgrade. */
	public static void onBossDefeated(ServerLevel level) {
		if (level.dimension() != DIMENSION) {
			return;
		}
		// No neighbor updates, so the soul fire is not knocked out for having netherrack under it.
		place(level, ALTAR.above(), Blocks.SOUL_FIRE.defaultBlockState());
		buildExitPortal(level);
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(Component.literal("A strange blue flame rises from the altar...").withStyle(ChatFormatting.AQUA));
		}
		level.playSound(null, ALTAR, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 2.0F, 0.8F);
	}

	/**
	 * A Netherite Upgrade smithing template thrown into the soul fire becomes a Chaos Upgrade. Checked
	 * every tick while the soul fire burns, because an item in fire is soon destroyed.
	 */
	private static void checkSoulFire(ServerLevel level) {
		BlockPos firePos = ALTAR.above();
		if (!level.getBlockState(firePos).is(Blocks.SOUL_FIRE)) {
			return;
		}
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(firePos).inflate(0.1))) {
			ItemStack stack = item.getItem();
			if (stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
				item.setItem(new ItemStack(ModItems.CHAOS_SMITHING_TEMPLATE, stack.getCount()));
				// Fire-proof, so the Chaos Upgrade is not burned up in the flame that made it.
				item.setInvulnerable(true);
				item.setRemainingFireTicks(0);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, item.getX(), item.getY() + 0.2, item.getZ(), 30, 0.2, 0.3, 0.2, 0.05);
				level.playSound(null, firePos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.5F);
			}
		}
	}
}

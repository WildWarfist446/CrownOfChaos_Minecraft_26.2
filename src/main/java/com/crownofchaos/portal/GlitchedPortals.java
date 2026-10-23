package com.crownofchaos.portal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.block.ModBlocks;
import com.crownofchaos.sound.ModSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The glitched portals: nether portals that a thrown Disc 11 has turned into doors to the Chaos Arena.
 * The portal's swirl becomes "null" blocks (see {@link com.crownofchaos.block.NullPortalBlock}), and its
 * obsidian frame becomes random blocks that never fall or update. Breaking a null block destroys the
 * whole portal. This remembers every one (saved with the world), so that is possible, and so that
 * the frame's blocks can be frozen (see {@code FallingBlockMixin}).
 */
public final class GlitchedPortals extends SavedData {
	/** One glitched portal: where it is, its null blocks and its frame. */
	public record Portal(UUID id, ResourceKey<Level> dimension, List<BlockPos> nulls, List<BlockPos> frame) {
		public static final Codec<Portal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Portal::id),
				Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Portal::dimension),
				BlockPos.CODEC.listOf().fieldOf("nulls").forGetter(Portal::nulls),
				BlockPos.CODEC.listOf().fieldOf("frame").forGetter(Portal::frame)
		).apply(instance, Portal::new));
	}

	private static final Codec<GlitchedPortals> CODEC = Portal.CODEC.listOf().xmap(GlitchedPortals::new, data -> List.copyOf(data.portals.values()));
	/** Vanilla needs a data fixer type; the command storage one changes nothing this data would notice. */
	public static final SavedDataType<GlitchedPortals> TYPE = new SavedDataType<>(
			CrownOfChaos.id("glitched_portals"), GlitchedPortals::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** The most blocks of swirl a portal can have (vanilla portals are at most 21 by 21). */
	private static final int MAX_PORTAL_BLOCKS = 21 * 21;

	/** What the frame's blocks can become: a different random one for each. */
	private static final List<Block> FRAME_BLOCKS = List.of(
			Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.NETHERRACK,
			Blocks.NETHER_BRICKS, Blocks.SOUL_SAND, Blocks.GLOWSTONE, Blocks.MAGMA_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.BLACKSTONE,
			Blocks.END_STONE, Blocks.BEDROCK, Blocks.SCULK, Blocks.PURPUR_BLOCK, Blocks.PRISMARINE, Blocks.SLIME_BLOCK);

	private final Map<UUID, Portal> portals = new LinkedHashMap<>();
	/** Every block of every portal, by dimension, for quick lookups. Built from {@link #portals}. */
	private final Map<ResourceKey<Level>, Map<Long, UUID>> index = new HashMap<>();

	public GlitchedPortals() {
	}

	private GlitchedPortals(List<Portal> list) {
		list.forEach(this::add);
	}

	public static GlitchedPortals get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	private void add(Portal portal) {
		this.portals.put(portal.id(), portal);
		Map<Long, UUID> blocks = this.index.computeIfAbsent(portal.dimension(), key -> new HashMap<>());
		for (BlockPos pos : portal.nulls()) {
			blocks.put(pos.asLong(), portal.id());
		}
		for (BlockPos pos : portal.frame()) {
			blocks.put(pos.asLong(), portal.id());
		}
	}

	private void remove(Portal portal) {
		this.portals.remove(portal.id());
		Map<Long, UUID> blocks = this.index.get(portal.dimension());
		if (blocks != null) {
			portal.nulls().forEach(pos -> blocks.remove(pos.asLong()));
			portal.frame().forEach(pos -> blocks.remove(pos.asLong()));
		}
	}

	/** Whether this spot is a block of a glitched portal (its null blocks or its frame). */
	public static boolean isGlitched(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		Map<Long, UUID> blocks = get(serverLevel.getServer()).index.get(serverLevel.dimension());
		return blocks != null && blocks.containsKey(pos.asLong());
	}

	/**
	 * Turns a nether portal into a glitched one: the swirl into null blocks and the obsidian frame into
	 * random blocks, placed without neighbor updates so nothing in them reacts. Returns whether it did.
	 */
	public static boolean convert(ServerLevel level, BlockPos start) {
		BlockState startState = level.getBlockState(start);
		if (!startState.is(Blocks.NETHER_PORTAL)) {
			return false;
		}
		Direction.Axis axis = startState.getValue(NetherPortalBlock.AXIS);
		Direction along = Direction.get(Direction.AxisDirection.POSITIVE, axis);

		// The swirl: every connected nether portal block in the same plane.
		Set<BlockPos> swirl = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		swirl.add(start);
		while (!queue.isEmpty() && swirl.size() <= MAX_PORTAL_BLOCKS) {
			BlockPos pos = queue.poll();
			for (BlockPos next : List.of(pos.relative(along), pos.relative(along.getOpposite()), pos.above(), pos.below())) {
				if (!swirl.contains(next) && level.getBlockState(next).is(Blocks.NETHER_PORTAL)) {
					swirl.add(next);
					queue.add(next);
				}
			}
		}
		if (swirl.size() > MAX_PORTAL_BLOCKS) {
			return false;
		}

		// The frame: the obsidian around the swirl, in its plane, corners included.
		Set<BlockPos> frame = new HashSet<>();
		for (BlockPos pos : swirl) {
			for (int across = -1; across <= 1; across++) {
				for (int up = -1; up <= 1; up++) {
					BlockPos candidate = pos.relative(along, across).above(up);
					if (!swirl.contains(candidate) && level.getBlockState(candidate).is(Blocks.OBSIDIAN)) {
						frame.add(candidate);
					}
				}
			}
		}

		BlockState nullBlock = ModBlocks.NULL_PORTAL.defaultBlockState();
		for (BlockPos pos : swirl) {
			level.setBlock(pos, nullBlock, Block.UPDATE_CLIENTS);
		}
		randomizeFrame(level, frame);
		get(level.getServer()).register(level, new ArrayList<>(swirl), new ArrayList<>(frame));

		BlockPos centre = start;
		level.playSound(null, centre, ModSounds.PORTAL_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, 60, 1.0, 1.5, 1.0, 0.1);
		return true;
	}

	/** Makes each frame block a random different block (never gravity-affected: the blocks are frozen, see {@code FallingBlockMixin}). */
	public static void randomizeFrame(ServerLevel level, Set<BlockPos> frame) {
		RandomSource random = level.getRandom();
		for (BlockPos pos : frame) {
			Block block = FRAME_BLOCKS.get(random.nextInt(FRAME_BLOCKS.size()));
			level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Remembers a glitched portal that was just built (the arena's exit uses this too). */
	public Portal register(ServerLevel level, List<BlockPos> nulls, List<BlockPos> frame) {
		Portal portal = new Portal(Mth.createInsecureUUID(level.getRandom()), level.dimension(), nulls, frame);
		this.add(portal);
		this.setDirty();
		return portal;
	}

	/** Destroys the whole portal that this null block belongs to: its null blocks and its frame. */
	public static void destroyAt(ServerLevel level, BlockPos pos) {
		GlitchedPortals data = get(level.getServer());
		Map<Long, UUID> blocks = data.index.get(level.dimension());
		UUID id = blocks == null ? null : blocks.get(pos.asLong());
		Portal portal = id == null ? null : data.portals.get(id);
		if (portal == null) {
			return;
		}
		data.remove(portal);
		data.setDirty();
		for (BlockPos blockPos : portal.nulls()) {
			clear(level, blockPos);
		}
		for (BlockPos blockPos : portal.frame()) {
			clear(level, blockPos);
		}
		level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.4F);
	}

	private static void clear(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.02);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** The portal (if any) with a block at this spot. */
	public @Nullable Portal portalAt(ResourceKey<Level> dimension, BlockPos pos) {
		Map<Long, UUID> blocks = this.index.get(dimension);
		UUID id = blocks == null ? null : blocks.get(pos.asLong());
		return id == null ? null : this.portals.get(id);
	}
}

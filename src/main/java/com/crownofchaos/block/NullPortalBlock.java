package com.crownofchaos.block;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.dimension.ChaosArena;
import com.crownofchaos.portal.GlitchedPortals;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The "null" block: the swirl of a glitched portal (see {@link GlitchedPortals}), drawn with Minecraft's
 * own missing-texture purple and black. Like a nether portal, standing in it for a few seconds takes
 * a player to the Chaos Arena. Breaking one destroys the whole portal.
 */
public class NullPortalBlock extends Block implements Portal {
	public static final MapCodec<NullPortalBlock> CODEC = simpleCodec(NullPortalBlock::new);

	/** Where a player came into the Chaos Arena from, so the arena's exit can take them back. */
	public static final AttachmentType<GlobalPos> ORIGIN = AttachmentRegistry.create(
			com.crownofchaos.CrownOfChaos.id("chaos_arena_origin"), builder -> builder.persistent(GlobalPos.CODEC));

	public NullPortalBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		// Only players go through; items and mobs just stand in it.
		if (entity instanceof Player && entity.canUsePortal(false)) {
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		return entity instanceof Player player
				? Math.max(0, level.getGameRules().get(player.getAbilities().invulnerable ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY))
				: 0;
	}

	@Override
	public Portal.Transition getLocalTransition() {
		return Portal.Transition.CONFUSION;
	}

	/** Into the arena, remembering where you came from. */
	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
		ServerLevel arena = currentLevel.getServer().getLevel(ChaosArena.DIMENSION);
		if (arena == null) {
			return null;
		}
		ChaosArena.ensureBuilt(arena);
		entity.setAttached(ORIGIN, GlobalPos.of(currentLevel.dimension(), portalEntryPos));
		Vec3 arrival = Vec3.atBottomCenterOf(ChaosArena.ARRIVAL);
		return new TeleportTransition(arena, arrival, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND);
	}

	/** Breaking any null block destroys the whole portal and its frame. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel serverLevel) {
			GlitchedPortals.destroyAt(serverLevel, pos);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** The null blocks of the arena's exit: the way back, which cannot be broken. */
	public static final class Exit extends NullPortalBlock {
		public static final MapCodec<Exit> EXIT_CODEC = simpleCodec(Exit::new);

		public Exit(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		protected MapCodec<? extends Block> codec() {
			return EXIT_CODEC;
		}

		/** Back to where the player came in, or to their respawn point if they have none (or it is gone). */
		@Override
		public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
			Optional<GlobalPos> origin = Optional.ofNullable(entity.getAttached(ORIGIN));
			ServerLevel level = origin.map(o -> currentLevel.getServer().getLevel(o.dimension())).orElse(null);
			if (level != null) {
				BlockPos spot = safeSpotNear(level, origin.get().pos());
				return new TeleportTransition(level, Vec3.atBottomCenterOf(spot), Vec3.ZERO, entity.getYRot(), 0.0F, TeleportTransition.PLAY_PORTAL_SOUND);
			}
			if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
				return TeleportTransition.createDefault(player, TeleportTransition.PLAY_PORTAL_SOUND);
			}
			return null;
		}

		/** Somewhere to stand next to where the portal was: air to stand in, a solid floor, and not a portal block. */
		private static BlockPos safeSpotNear(ServerLevel level, BlockPos portal) {
			for (int radius = 1; radius <= 4; radius++) {
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						for (int dy = -2; dy <= 2; dy++) {
							BlockPos pos = portal.offset(dx, dy, dz);
							if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
									&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
									&& !(level.getBlockState(pos.below()).getBlock() instanceof NullPortalBlock)) {
								return pos;
							}
						}
					}
				}
			}
			return portal.above();
		}
	}
}

package com.crownofchaos.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.crownofchaos.entity.IcicleEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Winter secondary ability: drops 5 icicles, one after another, on the mob or player
 * the wearer is looking at, within 30 blocks. Each icicle appears 5 blocks above the target's
 * head (lower under a ceiling), ahead of a moving target, hangs there for a moment and falls
 * straight down, so a target
 * that changes direction can dodge. Each one that hits deals freeze damage and freezes the
 * target (see {@link IcicleEntity}). Icicles never place blocks.
 */
public final class IcicleDropAbility implements CrownAbility {
	private static final double RANGE = 30.0;
	private static final int ICICLE_COUNT = 5;
	/** 0.35 seconds between icicles: 50% faster than the original half second. */
	private static final int TICKS_BETWEEN_ICICLES = 7;
	private static final double HEIGHT_ABOVE_HEAD = 5.0;
	/**
	 * Starting fall speed in blocks per tick; gravity speeds it up from there (see
	 * {@link IcicleEntity#GRAVITY}). 2.625 times the original 0.8: 40% faster, then 25% and 50% faster again.
	 */
	private static final double FALL_SPEED = 2.1;

	/** 6 seconds, counted from when the first icicle drops. */
	private static final int COOLDOWN_TICKS = 120;

	/** Icicle drops still in progress. Only touched on the server thread. */
	private static final List<Drop> DROPS = new ArrayList<>();

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		LivingEntity target = AbilityTargeting.findLookTarget(player, RANGE);
		if (target == null) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.icicle_drop.no_target")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		DROPS.add(new Drop(player, target));
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(IcicleDropAbility::tickDrops);
	}

	private static void tickDrops(MinecraftServer server) {
		Iterator<Drop> iterator = DROPS.iterator();
		while (iterator.hasNext()) {
			Drop drop = iterator.next();
			// Stop if the wearer or target is gone, or they're now in different dimensions.
			if (drop.player.isRemoved() || !drop.target.isAlive() || drop.target.level() != drop.player.level()) {
				iterator.remove();
				continue;
			}

			// Track how far the target moved this tick, so icicles can lead it. Measured from its
			// position because the server doesn't know a player's velocity reliably.
			Vec3 position = drop.target.position();
			Vec3 velocity = drop.lastPosition == null ? Vec3.ZERO : position.subtract(drop.lastPosition);
			drop.lastPosition = position;

			if (drop.ticks % TICKS_BETWEEN_ICICLES == 0) {
				spawnIcicle(drop, velocity);
				if (++drop.dropped >= ICICLE_COUNT) {
					iterator.remove();
					continue;
				}
			}
			drop.ticks++;
		}
	}

	private static void spawnIcicle(Drop drop, Vec3 velocity) {
		LivingEntity target = drop.target;
		ServerLevel level = (ServerLevel) target.level();
		double headTop = target.getY() + target.getBbHeight();

		// Under a ceiling, appear just below it instead, so the icicle isn't stuck in the blocks.
		double y = headTop + HEIGHT_ABOVE_HEAD;
		Vec3 above = new Vec3(target.getX(), headTop, target.getZ());
		BlockHitResult ceiling = level.clip(new ClipContext(above, new Vec3(above.x, y + IcicleEntity.HEIGHT, above.z),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		if (ceiling.getType() != HitResult.Type.MISS) {
			y = Math.max(headTop, ceiling.getLocation().y - IcicleEntity.HEIGHT);
		}

		// Appear ahead of a moving target by about how far it moves while the icicle hangs and falls.
		double fallTicks = IcicleEntity.HANG_TICKS + (y - target.getY()) / FALL_SPEED;
		double x = target.getX() + velocity.x * fallTicks;
		double z = target.getZ() + velocity.z * fallTicks;

		level.addFreshEntity(IcicleEntity.create(level, drop.player, x, y, z, FALL_SPEED));
		level.playSound(null, x, y, z, SoundEvents.POINTED_DRIPSTONE_FALL, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	private static final class Drop {
		private final ServerPlayer player;
		private final LivingEntity target;
		private int ticks;
		private int dropped;
		private Vec3 lastPosition;

		private Drop(ServerPlayer player, LivingEntity target) {
			this.player = player;
			this.target = target;
		}
	}
}

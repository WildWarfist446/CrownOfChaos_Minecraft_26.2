package com.crownofchaos.entity;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.ability.CrownAbilities;
import com.crownofchaos.item.CrownEffects;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An icicle from the Crown of Winter's Icicle Drop. It falls like a falling block and looks
 * like a thin shard of Packed Ice, but it never lands as a block: it shatters when it hits a
 * mob or player, or the ground, and deals freeze damage and freezes whatever it hits. It never
 * hurts the player who called it or their pets.
 */
public final class IcicleEntity extends FallingBlockEntity {
	public static final float WIDTH = 0.35F;
	public static final float HEIGHT = 1.0F;
	/** Freeze damage per icicle: 2 hearts. Mobs that freezing hurts extra, like Blazes, take 5 times this. */
	public static final float DAMAGE = 4.0F;
	/**
	 * Freezing an icicle adds on top of being fully frozen. Freezing thaws at 2 per tick, so the
	 * target stays fully frozen for 7 seconds, then thaws over 3.5 more. (Crown of Winter melee
	 * hits use {@link CrownEffects#WINTER_CROWN_EXTRA_FREEZE_TICKS}: 3.5 seconds.)
	 */
	public static final int EXTRA_FREEZE_TICKS = 280;
	/** The block the icicle looks like. */
	private static final BlockState LOOKS_LIKE = Blocks.PACKED_ICE.defaultBlockState();
	/** Hits mobs this far beyond the icicle's sides, so a near miss still counts. */
	private static final double HIT_MARGIN = 0.3;
	/** Shatters in the air if it's still falling after this long (5 seconds). */
	private static final int MAX_FALL_TICKS = 100;
	/**
	 * About 6.9 (2.625 squared) times a falling block's gravity of 0.04: with a 2.625 times faster
	 * start, this makes the whole fall 2.625 times as fast (40% faster, then 25% and 50% faster again).
	 */
	public static final double GRAVITY = 0.2756;
	/**
	 * Hangs in the air this long before it falls (a quarter second). The fall itself takes only
	 * about 4 ticks, so without this the icicles are gone almost as soon as they appear.
	 */
	public static final int HANG_TICKS = 5;

	/** Who called the icicle down. Not saved; neither is the icicle. */
	private @Nullable ServerPlayer owner;
	/**
	 * Set once it has hit something. It's removed a tick later, so players, who see it about a
	 * tick behind the server, see it reach what it hit instead of vanishing above it.
	 */
	private boolean shattered;

	public IcicleEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
		super(type, level);
		this.dropItem = false;
		this.disableDrop();
	}

	/** An icicle at the given spot, starting to fall at {@code fallSpeed} blocks per tick. */
	public static IcicleEntity create(ServerLevel level, ServerPlayer owner, double x, double y, double z, double fallSpeed) {
		IcicleEntity icicle = new IcicleEntity(ModEntities.ICICLE, level);
		icicle.owner = owner;
		icicle.setPos(x, y, z);
		icicle.xo = x;
		icicle.yo = y;
		icicle.zo = z;
		icicle.setStartPos(icicle.blockPosition());
		icicle.setDeltaMovement(0.0, -fallSpeed, 0.0);
		icicle.addTag(CrownAbilities.CROWN_ABILITY_TAG);
		return icicle;
	}

	@Override
	public BlockState getBlockState() {
		return LOOKS_LIKE;
	}

	@Override
	protected double getDefaultGravity() {
		return GRAVITY;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected Component getTypeName() {
		return Component.translatable(this.getType().getDescriptionId());
	}

	/**
	 * Replaces the falling block's tick, which would place the block when it lands. Players
	 * simulate the fall themselves between position updates, like a falling block.
	 */
	@Override
	public void tick() {
		this.time++;
		if (this.level() instanceof ServerLevel level
				&& (this.shattered || this.owner == null || this.owner.isRemoved() || this.owner.level() != level)) {
			this.discard();
			return;
		}
		if (this.time <= HANG_TICKS) {
			return;
		}

		AABB before = this.getBoundingBox();
		this.applyGravity();
		this.move(MoverType.SELF, this.getDeltaMovement());

		if (this.level() instanceof ServerLevel level) {
			// Everything it passed through this tick, so a fast icicle can't skip over a short mob.
			AABB path = before.minmax(this.getBoundingBox()).inflate(HIT_MARGIN, 0.0, HIT_MARGIN);
			boolean hitSomething = false;
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path, this::canHit)) {
				hitSomething = true;
				DamageSource source = level.damageSources().source(DamageTypes.FREEZE, this, this.owner);
				// Targets normally ignore further damage for half a second after a hit, which would
				// block every other icicle; like volley arrows, icicles skip that.
				target.invulnerableTime = 0;
				// Credited to the wearer, so kills count as theirs.
				if (target.hurtServer(level, source, DAMAGE) && target.canFreeze()) {
					int frozen = target.getTicksRequiredToFreeze() + EXTRA_FREEZE_TICKS;
					target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozen));
				}
			}

			if (hitSomething || this.onGround() || this.horizontalCollision || this.time > MAX_FALL_TICKS) {
				this.shatter(level);
				return;
			}
		}

		this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
	}

	private boolean canHit(LivingEntity target) {
		return target != this.owner && target.isAlive() && !target.isSpectator() && !CrownEffects.isPetOf(target, this.owner);
	}

	/** Breaks apart with the particles and sound of breaking ice; removed next tick. */
	private void shatter(ServerLevel level) {
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, this.blockPosition(), Block.getId(LOOKS_LIKE));
		this.setDeltaMovement(Vec3.ZERO);
		this.shattered = true;
	}
}

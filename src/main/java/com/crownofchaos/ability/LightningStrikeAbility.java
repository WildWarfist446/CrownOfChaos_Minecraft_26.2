package com.crownofchaos.ability;

import com.crownofchaos.item.CrownEffects;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Crown of Poseidon secondary ability: calls a slightly weaker lightning bolt down on the mob
 * or player the wearer is looking at, within 30 blocks. It looks and sounds like real lightning
 * but, compared with a natural strike (see vanilla {@code Entity#thunderHit}), deals 4 damage
 * instead of 5, burns for 4 seconds instead of 8, hits within 2 blocks instead of 3, never
 * sets blocks on fire, and doesn't transform mobs (no charged creepers or witches). It never
 * hurts the wearer or their pets.
 */
public final class LightningStrikeAbility implements CrownAbility {
	private static final double RANGE = 30.0;
	private static final float DAMAGE = 4.0F;
	private static final float BURN_SECONDS = 4.0F;
	/** Entities within this many blocks of the strike are hit. */
	private static final double HIT_RADIUS = 2.0;

	/** 20 seconds between uses. */
	private static final int COOLDOWN_TICKS = 400;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		LivingEntity target = AbilityTargeting.findLookTarget(player, RANGE);
		if (target == null) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.lightning.no_target")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		strike(player, target);
		return true;
	}

	/** Calls a bolt down on the target (the caller has checked it's in range and in sight). */
	static void strike(ServerPlayer player, LivingEntity target) {
		ServerLevel level = (ServerLevel) player.level();
		// A visual-only bolt gives the real look and sound without vanilla's damage, fire and
		// mob transformations; the damage is dealt below instead.
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) {
			return;
		}
		bolt.snapTo(target.getX(), target.getY(), target.getZ());
		bolt.setVisualOnly(true);
		bolt.addTag(CrownAbilities.CROWN_ABILITY_TAG);
		level.addFreshEntity(bolt);

		AABB area = new AABB(target.getX() - HIT_RADIUS, target.getY() - HIT_RADIUS, target.getZ() - HIT_RADIUS,
				target.getX() + HIT_RADIUS, target.getY() + target.getBbHeight() + HIT_RADIUS, target.getZ() + HIT_RADIUS);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (entity == player || CrownEffects.isPetOf(entity, player) || !entity.isAlive()) {
				continue;
			}
			// Credited to the wearer, so kills count as theirs.
			if (entity.hurtServer(level, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, bolt, player), DAMAGE)) {
				entity.igniteForSeconds(BURN_SECONDS);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

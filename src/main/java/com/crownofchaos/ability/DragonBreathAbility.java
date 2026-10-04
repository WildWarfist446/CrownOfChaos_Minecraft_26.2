package com.crownofchaos.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.crownofchaos.item.CrownEffects;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Crown of Void secondary ability: surrounds the wearer with the Ender Dragon's breath, shaped
 * like the lingering cloud its fireballs leave (see vanilla {@code DragonFireball#onHit}): it
 * starts 3 blocks wide and spreads to 7 over 30 seconds.
 *
 * <p>The dragon's cloud hurts through Instant Damage, which heals undead mobs. This one deals
 * the same damage (6, once a second) directly instead, so it hurts every mob and player
 * inside, undead included, but never the wearer or their pets. It can't be bottled (only the
 * Ender Dragon's breath can).
 */
public final class DragonBreathAbility implements CrownAbility {
	/** Entity tag marking clouds made by this ability. Saved with the entity. */
	public static final String CLOUD_TAG = "crownofchaos.void_crown_breath";

	/** What the dragon's Harming II cloud deals each time: 6 << 1 at the cloud's half strength. */
	private static final float DAMAGE = 6.0F;
	private static final int DAMAGE_INTERVAL_TICKS = 20;
	/** How high above the cloud entities are still caught in it. */
	private static final double CLOUD_HEIGHT = 2.5;

	/** 30 seconds between uses. */
	private static final int COOLDOWN_TICKS = 600;

	/** Breath clouds currently in loaded chunks. Only touched on the server thread. */
	private static final List<AreaEffectCloud> CLOUDS = new ArrayList<>();

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ServerLevel level = (ServerLevel) player.level();
		AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());
		cloud.setOwner(player);
		cloud.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
		cloud.setRadius(3.0F);
		cloud.setDuration(600);
		cloud.setRadiusPerTick((7.0F - cloud.getRadius()) / cloud.getDuration());
		cloud.addTag(CLOUD_TAG);
		cloud.addTag(CrownAbilities.CROWN_ABILITY_TAG);
		level.addFreshEntity(cloud);

		level.playSound(null, player, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	static void initialize() {
		// Picks up new clouds and ones loaded back in with their chunk.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof AreaEffectCloud cloud && cloud.entityTags().contains(CLOUD_TAG)) {
				CLOUDS.add(cloud);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<AreaEffectCloud> iterator = CLOUDS.iterator();
			while (iterator.hasNext()) {
				AreaEffectCloud cloud = iterator.next();
				if (cloud.isRemoved()) {
					iterator.remove();
				} else if (cloud.tickCount >= cloud.getWaitTime() && cloud.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
					hurtEntitiesInside(cloud);
				}
			}
		});
	}

	private static void hurtEntitiesInside(AreaEffectCloud cloud) {
		ServerLevel level = (ServerLevel) cloud.level();
		LivingEntity owner = cloud.getOwner();
		double radius = cloud.getRadius();
		AABB area = new AABB(cloud.getX() - radius, cloud.getY() - 0.5, cloud.getZ() - radius,
				cloud.getX() + radius, cloud.getY() + CLOUD_HEIGHT, cloud.getZ() + radius);

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			double dx = entity.getX() - cloud.getX();
			double dz = entity.getZ() - cloud.getZ();
			if (entity != owner && !CrownEffects.isPetOf(entity, owner) && entity.isAlive() && !entity.isSpectator()
					&& dx * dx + dz * dz <= radius * radius) {
				entity.hurtServer(level, level.damageSources().indirectMagic(cloud, owner), DAMAGE);
			}
		}
	}
}

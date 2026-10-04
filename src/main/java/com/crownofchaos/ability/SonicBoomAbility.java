package com.crownofchaos.ability;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Darkness ability: the Warden's sonic boom (see vanilla {@code SonicBoom}). The
 * target is whatever the player is looking at. Like the Warden, it charges for 1.7 seconds,
 * then hits only that target for 10 armor-piercing damage with heavy knockback, even through
 * walls, as long as the target is still in range.
 */
public final class SonicBoomAbility implements CrownAbility {
	/** The Warden's range: 15 blocks horizontally, 20 vertically. */
	private static final double RANGE_XZ = 15.0;
	private static final double RANGE_Y = 20.0;
	private static final int CHARGE_TICKS = 34;
	private static final float DAMAGE = 10.0F;
	private static final double KNOCKBACK_HORIZONTAL = 2.5;
	private static final double KNOCKBACK_VERTICAL = 0.5;

	/** 15 seconds, counted from when the charge starts. */
	private static final int COOLDOWN_TICKS = 300;

	/** Booms that are charging, by player. Only touched on the server thread. */
	private static final Map<UUID, Charge> CHARGING = new HashMap<>();

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		LivingEntity target = findTarget(player);
		if (target == null) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.sonic_boom.no_target")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		player.level().playSound(null, player, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3.0F, 1.0F);
		CHARGING.put(player.getUUID(), new Charge(target));
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(SonicBoomAbility::tickCharges);
	}

	private static void tickCharges(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Charge>> iterator = CHARGING.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Charge> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			// The charge fizzles if the player leaves, dies or takes the crown off.
			if (player == null || !player.isAlive() || !CrownEffects.isWearing(player, ModItems.CROWN_OF_DARKNESS)) {
				iterator.remove();
				continue;
			}

			Charge charge = entry.getValue();
			if (--charge.ticksLeft <= 0) {
				iterator.remove();
				fire(player, charge.target);
			}
		}
	}

	private static void fire(ServerPlayer player, LivingEntity target) {
		if (!target.isAlive() || target.level() != player.level() || !player.closerThan(target, RANGE_XZ, RANGE_Y)) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		// The Warden fires from its chest; for a player that's just below eye level.
		Vec3 source = player.getEyePosition().subtract(0.0, 0.4, 0.0);
		Vec3 delta = target.getEyePosition().subtract(source);
		Vec3 direction = delta.normalize();
		int steps = Mth.floor(delta.length()) + 7;
		for (int i = 1; i < steps; i++) {
			Vec3 pos = source.add(direction.scale(i));
			level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		level.playSound(null, player, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
		if (target.hurtServer(level, level.damageSources().sonicBoom(player), DAMAGE)) {
			double resistance = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			target.push(direction.x * KNOCKBACK_HORIZONTAL * resistance,
					direction.y * KNOCKBACK_VERTICAL * resistance,
					direction.z * KNOCKBACK_HORIZONTAL * resistance);
		}
	}

	/** The living entity under the player's crosshair, if it's in sight and within the Warden's range. */
	private static LivingEntity findTarget(ServerPlayer player) {
		LivingEntity target = AbilityTargeting.findLookTarget(player, RANGE_Y);
		return target != null && player.closerThan(target, RANGE_XZ, RANGE_Y) ? target : null;
	}

	private static final class Charge {
		private final LivingEntity target;
		private int ticksLeft = CHARGE_TICKS;

		private Charge(LivingEntity target) {
			this.target = target;
		}
	}
}

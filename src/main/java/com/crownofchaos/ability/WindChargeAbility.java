package com.crownofchaos.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Crown of Sun ability: throws a wind charge, exactly like using a wind charge item, without
 * needing one. The crown holds up to 3 charges, and spent charges come back one at a time,
 * 6 seconds each. Every throw shows the charges left in the action bar.
 */
public final class WindChargeAbility implements CrownAbility {
	private static final int MAX_CHARGES = 3;
	private static final int RECHARGE_TICKS = 120;

	/** Same half-second delay between throws as the wind charge item. */
	private static final int THROW_DELAY_TICKS = 10;

	/** Charge state per player. Kept across relogs so leaving doesn't refill charges. */
	private static final Map<UUID, Charges> CHARGES = new HashMap<>();

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ServerLevel level = (ServerLevel) player.level();
		int now = level.getServer().getTickCount();
		Charges charges = CHARGES.computeIfAbsent(player.getUUID(), uuid -> new Charges(now));
		charges.recharge(now);

		if (now - charges.lastThrowTick < THROW_DELAY_TICKS) {
			return false;
		}

		if (charges.count == 0) {
			int secondsLeft = Mth.ceil((RECHARGE_TICKS - (now - charges.rechargeStartTick)) / 20.0F);
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wind_charge.empty", secondsLeft)
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		// A full crown starts recharging from the first throw.
		if (charges.count == MAX_CHARGES) {
			charges.rechargeStartTick = now;
		}
		charges.count--;
		charges.lastThrowTick = now;

		// The same throw as WindChargeItem#use.
		Projectile.spawnProjectileFromRotation(
				(source, l, stack) -> new WindCharge(player, level, player.position().x(), player.getEyePosition().y(), player.position().z()),
				level, new ItemStack(Items.WIND_CHARGE), player, 0.0F, 1.5F, 1.0F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

		player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wind_charge.charges", charges.count, MAX_CHARGES)
				.withStyle(CrownAbilities.MESSAGE_COLOR));
		// Charges are tracked here rather than with the crown's item cooldown.
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private static final class Charges {
		private int count = MAX_CHARGES;
		private int rechargeStartTick;
		private int lastThrowTick = Integer.MIN_VALUE / 2;

		private Charges(int now) {
			this.rechargeStartTick = now;
		}

		/** Restores every charge that has finished recharging since the last check. */
		private void recharge(int now) {
			// The server's tick counter restarts with the server.
			if (now < rechargeStartTick || now < lastThrowTick) {
				rechargeStartTick = now;
				lastThrowTick = Integer.MIN_VALUE / 2;
			}
			while (count < MAX_CHARGES && now - rechargeStartTick >= RECHARGE_TICKS) {
				count++;
				rechargeStartTick += RECHARGE_TICKS;
			}
		}
	}
}

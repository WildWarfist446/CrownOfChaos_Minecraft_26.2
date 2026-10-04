package com.crownofchaos.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Tracks charges for abilities that hold several uses at once, like Wind Charge and Wither
 * Skull. Spent charges come back one at a time, and there is a minimum delay between uses.
 * State is kept per player, including across relogs so leaving doesn't refill charges.
 */
final class ChargeTracker {
	enum Status {
		/** A charge was spent; {@link UseResult#charges()} is how many are left. */
		USED,
		/** No charges left; {@link UseResult#seconds()} is the time until the next one. */
		EMPTY,
		/** Used too recently; {@link UseResult#seconds()} is the time until it can be used again. */
		TOO_SOON
	}

	record UseResult(Status status, int charges, int seconds) {
	}

	private final int maxCharges;
	private final int rechargeTicks;
	private final int delayTicks;
	private final Map<UUID, Charges> charges = new HashMap<>();

	ChargeTracker(int maxCharges, int rechargeTicks, int delayTicks) {
		this.maxCharges = maxCharges;
		this.rechargeTicks = rechargeTicks;
		this.delayTicks = delayTicks;
	}

	int maxCharges() {
		return maxCharges;
	}

	/** Spends a charge if one is available and the delay since the last use has passed. */
	UseResult tryUse(ServerPlayer player) {
		int now = player.level().getServer().getTickCount();
		Charges state = charges.computeIfAbsent(player.getUUID(), uuid -> new Charges(now));
		state.recharge(now);

		int sinceLastUse = now - state.lastUseTick;
		if (sinceLastUse < delayTicks) {
			return new UseResult(Status.TOO_SOON, state.count, Mth.ceil((delayTicks - sinceLastUse) / 20.0F));
		}

		if (state.count == 0) {
			return new UseResult(Status.EMPTY, 0, Mth.ceil((rechargeTicks - (now - state.rechargeStartTick)) / 20.0F));
		}

		// A full set starts recharging from the first use.
		if (state.count == maxCharges) {
			state.rechargeStartTick = now;
		}
		state.count--;
		state.lastUseTick = now;
		return new UseResult(Status.USED, state.count, 0);
	}

	private final class Charges {
		private int count = maxCharges;
		private int rechargeStartTick;
		private int lastUseTick = Integer.MIN_VALUE / 2;

		private Charges(int now) {
			this.rechargeStartTick = now;
		}

		/** Restores every charge that has finished recharging since the last check. */
		private void recharge(int now) {
			// The server's tick counter restarts with the server.
			if (now < rechargeStartTick || now < lastUseTick) {
				rechargeStartTick = now;
				lastUseTick = Integer.MIN_VALUE / 2;
			}
			while (count < maxCharges && now - rechargeStartTick >= rechargeTicks) {
				count++;
				rechargeStartTick += rechargeTicks;
			}
		}
	}
}

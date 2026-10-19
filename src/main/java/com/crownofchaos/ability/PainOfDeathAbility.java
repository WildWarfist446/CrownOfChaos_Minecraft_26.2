package com.crownofchaos.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Crown of Chaos ability, "The pain of death": the Crown of Iron's Arrow Volley, the Crown of
 * Wither's Wither Skull, the Crown of Nether's Fire Charge and the Crown of Poseidon's Lightning
 * Strike, all at once.
 *
 * <p>The skull and the fireball fly where the wearer is looking. The volley and the lightning
 * go to the mob or player under the crosshair (the lightning within 30 blocks, the volley within
 * its usual 20). With no target the skull and fireball still fire. These shots don't use the other
 * crowns' charges or cooldowns, only this ability's own.
 */
public final class PainOfDeathAbility implements CrownAbility {
	/** How far away the lightning can strike a target, like the Crown of Poseidon's. */
	private static final double LIGHTNING_RANGE = 30.0;

	/** 10 seconds between uses. */
	private static final int COOLDOWN_TICKS = 200;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		LivingEntity target = AbilityTargeting.findLookTarget(player, LIGHTNING_RANGE);
		if (target != null) {
			LightningStrikeAbility.strike(player, target);
			if (player.distanceTo(target) <= ArrowVolleyAbility.range()) {
				ArrowVolleyAbility.begin(player, target);
			}
		}
		WitherSkullAbility.shoot(player);
		FireChargeAbility.shoot(player);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}
}

package com.crownofchaos.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A power a crown can trigger with the "Use Crown Ability" keybind. Abilities always run on
 * the server, and only after {@link CrownAbilities} has checked that the player is wearing
 * the crown and that it is off cooldown.
 */
public interface CrownAbility {
	/**
	 * Performs the ability.
	 *
	 * @param player the player wearing the crown
	 * @param crown  the crown stack in the player's head slot
	 * @return true if the ability fired and its cooldown should start, false if it couldn't
	 *         be used right now (e.g. no valid target), in which case no cooldown is applied
	 */
	boolean activate(ServerPlayer player, ItemStack crown);

	/** Cooldown after a successful activation, in ticks (20 ticks = 1 second). */
	int cooldownTicks();
}

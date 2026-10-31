package com.crownofchaos.item;

import com.crownofchaos.entity.ChaosKing;
import com.crownofchaos.unit.ModUnits;
import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A crown makes the hostile mobs it can make units of leave its wearer alone: a Crown of Wither wearer is ignored
 * by Wither Skeletons, a Crown of Void wearer by Endermen and Shulkers, and so on (the Crown of Chaos, which
 * makes every unit, by all of them). Applied through {@code Entity.isAlliedTo}, which every vanilla mob checks
 * before targeting anything (see {@link com.crownofchaos.mixin.EntityAlliesMixin}). Hit one of them, though, and
 * it fights back for a while. The Chaos King's legion and army units are not covered.
 */
public final class CrownPassivity {
	/** How long (10 seconds) a mob that was hurt by the wearer is allowed to fight back. */
	public static final int PROVOKED_TICKS = 200;

	private CrownPassivity() {
	}

	/** Whether this mob should treat this player as an ally because of the crown they are wearing. */
	public static boolean isPassiveTo(Mob mob, Player player) {
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		if (head.isEmpty() || !ModItems.isCrown(head.getItem()) || ChaosKing.isLegion(mob) || mob.hasAttached(UnitManager.UNIT)) {
			return false;
		}
		// The Crown of Chaos calms every hostile mob (Warfist, 31 October 2026), not only the ones it can make units of.
		boolean matches = (ModItems.baseCrown(head.getItem()) == ModItems.CROWN_OF_CHAOS && mob instanceof Enemy)
				|| ModUnits.forCrown(head.getItem()).stream().anyMatch(unit -> unit.entity() == mob.getType());
		return matches && !(mob.getLastHurtByMob() == player && mob.tickCount - mob.getLastHurtByMobTimestamp() <= PROVOKED_TICKS);
	}
}

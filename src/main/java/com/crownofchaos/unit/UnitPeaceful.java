package com.crownofchaos.unit;

import net.minecraft.world.entity.EntityType;

/**
 * Lets army units exist on Peaceful difficulty, where vanilla refuses to create hostile mobs (see
 * {@link com.crownofchaos.mixin.EntityTypeMixin}) and removes the ones that exist (see
 * {@link com.crownofchaos.mixin.MobDespawnMixin}).
 */
public final class UnitPeaceful {
	/** Greater than zero while the unit item is summoning a unit. Only the server thread touches it. */
	private static int summoning;

	private UnitPeaceful() {
	}

	public static boolean isSummoningUnit() {
		return summoning > 0;
	}

	/** Runs the summon with the unit exemption on. */
	public static <T> T summoning(java.util.function.Supplier<T> summon) {
		summoning++;
		try {
			return summon.get();
		} finally {
			summoning--;
		}
	}

	/** Whether this is a mob a Commander's Table can make a unit of. */
	public static boolean isUnitType(EntityType<?> type) {
		for (UnitType unit : ModUnits.all()) {
			if (unit.entity() == type) {
				return true;
			}
		}
		return false;
	}
}

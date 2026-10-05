package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/** Gives army units their extra goals (see {@code UnitManager}). */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("targetSelector")
	GoalSelector crownofchaos$targetSelector();

	@Accessor("goalSelector")
	GoalSelector crownofchaos$goalSelector();
}

package com.crownofchaos.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.crownofchaos.unit.UnitManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

/** Warden units never give nearby players Darkness (wild Wardens still do). */
@Mixin(Warden.class)
public abstract class WardenDarknessMixin {
	@WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/monster/warden/Warden;applyDarknessAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;I)V"))
	private void crownofchaos$noDarknessFromUnits(ServerLevel level, Vec3 position, @Nullable Entity source, int radius, Operation<Void> original) {
		if (!((Warden) (Object) this).hasAttached(UnitManager.UNIT)) {
			original.call(level, position, source, radius);
		}
	}
}

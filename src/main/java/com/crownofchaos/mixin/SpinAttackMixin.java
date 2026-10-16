package com.crownofchaos.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.crownofchaos.unit.UnitManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A Riptide spin attack (including the Crown of Poseidon's) stops dead, and bounces back, the
 * moment it touches any living thing. A player's own faction units, which crowd around and teleport
 * to them, were stopping the dash, so the spin attack passes through them instead. Runs on both
 * sides, since the dash is moved by the client.
 */
@Mixin(LivingEntity.class)
public abstract class SpinAttackMixin {
	@WrapOperation(method = "checkAutoSpinAttack",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<Entity> crownofchaos$passThroughOwnUnits(Level level, Entity self, AABB box, Operation<List<Entity>> original) {
		List<Entity> touched = original.call(level, self, box);
		if (self instanceof Player player && !touched.isEmpty()) {
			return touched.stream().filter(entity -> !UnitManager.isOwnFactionUnit(player, entity)).toList();
		}
		return touched;
	}
}

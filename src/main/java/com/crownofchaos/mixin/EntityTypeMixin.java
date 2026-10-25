package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.unit.UnitPeaceful;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * On Peaceful, vanilla refuses to create hostile mobs at all, so a Husk or Wither Skeleton unit could not be
 * summoned, and one saved in the world would not load back. Army units are exempt: the unit item says so while
 * it summons one, saved units of unit types load as usual, and a client always builds what the server tells it
 * about (the server decides). Every other hostile mob is refused on Peaceful as before.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> {
	@Shadow
	@Final
	private EntityType.EntityFactory<T> factory;

	@Shadow
	public abstract boolean isAllowedInPeaceful();

	@Inject(method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$unitsOnPeaceful(Level level, EntitySpawnRequest request, CallbackInfoReturnable<T> cir) {
		if (level.getDifficulty() != Difficulty.PEACEFUL || this.isAllowedInPeaceful()) {
			return;
		}
		boolean allowed = level.isClientSide()
				|| UnitPeaceful.isSummoningUnit()
				|| request.reason() == EntitySpawnReason.LOAD && UnitPeaceful.isUnitType((EntityType<?>) (Object) this);
		if (allowed) {
			cir.setReturnValue(this.factory.create((EntityType<T>) (Object) this, level));
		}
	}
}

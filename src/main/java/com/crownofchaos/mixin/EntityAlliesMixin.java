package com.crownofchaos.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crownofchaos.entity.ChaosKing;
import com.crownofchaos.item.CrownPassivity;
import com.crownofchaos.unit.UnitManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Makes army units passive: a unit counts everyone as an ally except those the faction rules
 * say to fight (see {@link UnitManager}). Every vanilla mob checks this before targeting
 * anything, including through its "brain".
 */
@Mixin(Entity.class)
public abstract class EntityAlliesMixin {
	@Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$unitAllies(@Nullable Entity other, CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (other == null || self.level().isClientSide()) {
			return;
		}
		if (self.hasAttached(UnitManager.UNIT)) {
			cir.setReturnValue(UnitManager.isAlly(self, other));
		} else if (ChaosKing.isLegion(self) && ChaosKing.isLegion(other)) {
			// The Chaos King's legion never turns on itself.
			cir.setReturnValue(true);
		} else if (self instanceof Mob mob && other instanceof Player player && CrownPassivity.isPassiveTo(mob, player)) {
			// A crown makes the hostile mobs it can make units of leave its wearer alone.
			cir.setReturnValue(true);
		}
	}
}

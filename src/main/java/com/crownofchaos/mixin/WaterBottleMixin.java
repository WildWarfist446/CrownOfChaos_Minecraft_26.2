package com.crownofchaos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * A Crown of Poseidon wearer who drinks a plain water bottle on land gets a strong Regeneration potion's
 * effect (in water the crown already regenerates them).
 */
@Mixin(LivingEntity.class)
public abstract class WaterBottleMixin {
	@Inject(method = "completeUsingItem", at = @At("HEAD"))
	private void crownofchaos$waterBottleRegeneration(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player) || !player.isUsingItem() || player.isInWater()
				|| !CrownEffects.isWearing(player, ModItems.CROWN_OF_POSEIDON)) {
			return;
		}
		ItemStack drink = player.getUseItem();
		PotionContents contents = drink.get(DataComponents.POTION_CONTENTS);
		if (contents != null && contents.is(Potions.WATER)) {
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, CrownEffects.POSEIDON_BOTTLE_REGENERATION_TICKS,
					CrownEffects.POSEIDON_BOTTLE_REGENERATION_AMPLIFIER));
		}
	}
}

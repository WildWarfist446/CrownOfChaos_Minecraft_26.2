package com.crownofchaos.mixin;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Makes block drops that scale with Fortune (ores, crops, etc.) treat the Under Crown's
 * wearer as having at least {@link CrownEffects.UNDER_CROWN_FORTUNE_LEVEL} Fortune on their tool.
 */
@Mixin(ApplyBonusCount.class)
public abstract class ApplyBonusCountMixin {
	@Shadow
	@Final
	private Holder<Enchantment> enchantment;

	@ModifyExpressionValue(
			method = "run",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I"))
	private int crownofchaos$underCrownFortune(int level, @Local(argsOnly = true) LootContext context) {
		if (!this.enchantment.is(Enchantments.FORTUNE)) {
			return level;
		}

		Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (entity instanceof LivingEntity living && CrownEffects.isWearing(living, ModItems.UNDER_CROWN)) {
			return Math.max(level, CrownEffects.UNDER_CROWN_FORTUNE_LEVEL);
		}

		return level;
	}
}

package com.crownofchaos.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Crown of Sun ability: throws a wind charge, exactly like using a wind charge item, without
 * needing one. The crown holds up to 3 charges, and spent charges come back one at a time,
 * 6 seconds each. Every throw shows the charges left in the action bar.
 */
public final class WindChargeAbility implements CrownAbility {
	/** 3 charges, 6 seconds each to recharge, and the wind charge item's half-second delay between throws. */
	private static final ChargeTracker CHARGES = new ChargeTracker(3, 120, 10);

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ChargeTracker.UseResult result = CHARGES.tryUse(player);
		switch (result.status()) {
			// The delay is as short as the item's own, so early presses are just ignored.
			case TOO_SOON -> {
				return false;
			}
			case EMPTY -> {
				player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wind_charge.empty", result.seconds())
						.withStyle(CrownAbilities.MESSAGE_COLOR));
				return false;
			}
			case USED -> {
			}
		}

		// The same throw as WindChargeItem#use.
		ServerLevel level = (ServerLevel) player.level();
		WindCharge windCharge = Projectile.spawnProjectileFromRotation(
				(source, l, stack) -> new WindCharge(player, level, player.position().x(), player.getEyePosition().y(), player.position().z()),
				level, new ItemStack(Items.WIND_CHARGE), player, 0.0F, 1.5F, 1.0F);
		windCharge.addTag(CrownAbilities.CROWN_ABILITY_TAG);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

		player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wind_charge.charges", result.charges(), CHARGES.maxCharges())
				.withStyle(CrownAbilities.MESSAGE_COLOR));
		// Charges are tracked by the ChargeTracker rather than the crown's item cooldown.
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

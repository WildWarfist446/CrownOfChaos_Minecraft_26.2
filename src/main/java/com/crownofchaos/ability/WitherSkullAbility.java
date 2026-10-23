package com.crownofchaos.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Wither ability: shoots a wither skull where the player is looking, like the
 * Wither does: 8 damage and Wither II on a hit (by difficulty), and the shooter heals if it
 * kills. Its explosion doesn't break blocks (see {@link com.crownofchaos.mixin.WitherSkullMixin}).
 * The crown holds up to 3 skulls, each taking 6 seconds to recharge, with 3 seconds between
 * shots. Every shot shows the skulls left in the action bar.
 */
public final class WitherSkullAbility implements CrownAbility {
	/** Entity tag marking skulls shot by this ability. Saved with the entity. */
	public static final String SKULL_TAG = "crownofchaos.wither_crown_skull";

	/** 3 charges, 6 seconds each to recharge, 3 seconds between shots. */
	private static final ChargeTracker CHARGES = new ChargeTracker(3, 120, 60);

	/** Level event for the Wither's shooting sound. */
	private static final int WITHER_SHOOT_EVENT = 1024;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		ChargeTracker.UseResult result = CHARGES.tryUse(player);
		switch (result.status()) {
			case TOO_SOON -> {
				player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wither_skull.too_soon", result.seconds())
						.withStyle(CrownAbilities.MESSAGE_COLOR));
				return false;
			}
			case EMPTY -> {
				player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wither_skull.empty", result.seconds())
						.withStyle(CrownAbilities.MESSAGE_COLOR));
				return false;
			}
			case USED -> {
			}
		}

		shoot(player);

		player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.wither_skull.charges", result.charges(), CHARGES.maxCharges())
				.withStyle(CrownAbilities.MESSAGE_COLOR));
		// Charges are tracked by the ChargeTracker rather than the crown's item cooldown.
		return false;
	}

	/** Fires one skull where the player is looking, without using a charge (the caller has checked those, if any). */
	static void shoot(ServerPlayer player) {
		shoot(player, player.getLookAngle());
	}

	/** Fires one skull from the shooter's head in a direction (the Chaos King aims it at his target). */
	public static void shoot(LivingEntity shooter, Vec3 direction) {
		// Like WitherBoss#performRangedAttack, fired from the shooter's head instead of the Wither's.
		WitherSkull skull = new WitherSkull(shooter.level(), shooter, direction.normalize());
		skull.setOwner(shooter);
		Vec3 start = shooter.getEyePosition().add(direction.normalize().scale(0.5));
		skull.setPos(start.x, start.y, start.z);
		skull.addTag(SKULL_TAG);
		skull.addTag(CrownAbilities.CROWN_ABILITY_TAG);
		shooter.level().addFreshEntity(skull);
		shooter.level().levelEvent(null, WITHER_SHOOT_EVENT, shooter.blockPosition(), 0);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

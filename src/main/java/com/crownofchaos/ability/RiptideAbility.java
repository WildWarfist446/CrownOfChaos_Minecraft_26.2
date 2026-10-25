package com.crownofchaos.ability;

import com.crownofchaos.network.RiptidePayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Crown of Poseidon ability: a Riptide III launch, identical to releasing a Riptide III
 * trident (see {@code TridentItem#releaseUsing}). Like the trident, it only works in water
 * or rain and not while riding.
 */
public final class RiptideAbility implements CrownAbility {
	/** Riptide's launch strength is 1.5 + 0.75 per level above I, so 3.0 at level III. */
	private static final float STRENGTH = 3.0F;
	private static final int SPIN_ATTACK_TICKS = 20;
	private static final float SPIN_ATTACK_DAMAGE = 8.0F;

	/** 4.5 seconds between launches. */
	private static final int COOLDOWN_TICKS = 45;

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		if (!player.isInWaterOrRain() || player.isPassenger()) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.riptide.needs_water")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		// Vanilla runs the launch on both sides: the client moves the player, and the server
		// keeps its copy in step and handles spin attack damage.
		launch(player, crown, STRENGTH);
		ServerPlayNetworking.send(player, new RiptidePayload(STRENGTH));
		player.level().playSound(null, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	/** The movement half of {@code TridentItem#releaseUsing}, run on both client and server. */
	public static void launch(Player player, ItemStack crown, float strength) {
		float yRot = player.getYRot();
		float xRot = player.getXRot();
		float xd = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
		float yd = -Mth.sin(xRot * Mth.DEG_TO_RAD);
		float zd = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
		float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
		xd *= strength / dist;
		yd *= strength / dist;
		zd *= strength / dist;
		player.push(xd, yd, zd);
		player.startAutoSpinAttack(SPIN_ATTACK_TICKS, SPIN_ATTACK_DAMAGE, crown);
		if (player.onGround()) {
			player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
		}
	}
}

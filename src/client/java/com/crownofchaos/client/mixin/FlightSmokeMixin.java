package com.crownofchaos.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.item.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;

/**
 * The Crown of Nether and Crown of Wither wearers' flight smoke (sent by the server to everyone) hides
 * the view in first person, so the wearer's own game drops it while the camera is first person. Other
 * players, and the wearer in third person, still see it.
 */
@Mixin(ClientPacketListener.class)
public abstract class FlightSmokeMixin {
	/** How far from the wearer's eyes the smoke can be and still count as theirs. */
	private static final double OWN_SMOKE_DISTANCE = 2.0;

	@Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
	private void crownofchaos$hideOwnFlightSmoke(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
		if (packet.getParticle() != ParticleTypes.SMOKE && packet.getParticle() != ParticleTypes.LARGE_SMOKE) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !Minecraft.getInstance().options.getCameraType().isFirstPerson()
				|| !(CrownEffects.isWearing(player, ModItems.CROWN_OF_NETHER) || CrownEffects.isWearing(player, ModItems.CROWN_OF_WITHER))) {
			return;
		}
		double distance = player.getEyePosition().distanceToSqr(packet.getX(), packet.getY(), packet.getZ());
		if (distance < OWN_SMOKE_DISTANCE * OWN_SMOKE_DISTANCE + player.getBbHeight() * player.getBbHeight()) {
			ci.cancel();
		}
	}
}

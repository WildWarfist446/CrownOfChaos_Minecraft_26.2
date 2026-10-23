package com.crownofchaos.client;

import com.crownofchaos.dimension.ChaosArena;
import com.crownofchaos.network.BossMusicPayload;
import com.crownofchaos.sound.ModSounds;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The Chaos King's fight music: a looping, non-positional track that the server starts and stops, and
 * that also stops by itself if the player leaves the Chaos Arena.
 */
public final class BossMusicClient {
	private static BossMusic current;

	private BossMusicClient() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(BossMusicPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			stop(client);
			if (payload.play()) {
				current = new BossMusic();
				client.getSoundManager().play(current);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stop(client));
	}

	private static void stop(Minecraft client) {
		if (current != null) {
			client.getSoundManager().stop(current);
			current = null;
		}
	}

	private static final class BossMusic extends AbstractTickableSoundInstance {
		BossMusic() {
			super(ModSounds.BOSS_MUSIC, SoundSource.MUSIC, RandomSource.create());
			this.looping = true;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft client = Minecraft.getInstance();
			if (client.level == null || client.level.dimension() != ChaosArena.DIMENSION) {
				this.stop();
			}
		}
	}
}

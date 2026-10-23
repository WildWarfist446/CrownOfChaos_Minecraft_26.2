package com.crownofchaos.sound;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** The mod's sounds, defined in {@code sounds.json}. */
public final class ModSounds {
	/** Played when a Disc 11 turns a nether portal into a glitched one. */
	public static final SoundEvent PORTAL_ACTIVATE = register("portal_activate");
	/** "Empire" by Laudividni: the Chaos King's fight music (see CREDITS.md). Loops until he is beaten. */
	public static final SoundEvent BOSS_MUSIC = register("boss_music");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, CrownOfChaos.id(name), SoundEvent.createVariableRangeEvent(CrownOfChaos.id(name)));
	}

	/** Loads this class so the sounds are registered. */
	public static void initialize() {
	}
}

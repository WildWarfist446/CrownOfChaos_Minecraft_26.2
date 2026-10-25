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
	/** "Celebration" by Laudividni ft. TenM: plays once after the Chaos King is beaten (see CREDITS.md). */
	public static final SoundEvent VICTORY_MUSIC = register("victory_music");
	/** The Chaos King's evil laugh: when a player dies, when his abilities hit, and when a wave starts. */
	public static final SoundEvent CHAOS_KING_LAUGH = register("chaos_king_laugh");
	/** His death wail, when he is finally killed. */
	public static final SoundEvent CHAOS_KING_DEATH = register("chaos_king_death");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, CrownOfChaos.id(name), SoundEvent.createVariableRangeEvent(CrownOfChaos.id(name)));
	}

	/** Loads this class so the sounds are registered. */
	public static void initialize() {
	}
}

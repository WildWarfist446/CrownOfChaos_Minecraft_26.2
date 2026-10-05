package com.crownofchaos.faction;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * How a faction treats another faction in its territory. Its units use this (see
 * {@code UnitManager}): allowed factions may enter unharmed, targeted ones are attacked, and
 * neutral ones are only attacked while the faction is on red alert.
 */
public enum FactionStance implements StringRepresentable {
	ALLOWED("allowed"),
	NEUTRAL("neutral"),
	TARGETED("targeted");

	public static final Codec<FactionStance> CODEC = StringRepresentable.fromEnum(FactionStance::values);

	private final String name;

	FactionStance(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	/** The next stance when cycling through them in the faction menu. */
	public FactionStance next() {
		return values()[(this.ordinal() + 1) % values().length];
	}
}

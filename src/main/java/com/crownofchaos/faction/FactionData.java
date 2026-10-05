package com.crownofchaos.faction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.CrownOfChaos;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Every faction in the world, saved with the world in {@code data/crownofchaos/factions.dat}. */
public final class FactionData extends SavedData {
	private static final Codec<FactionData> CODEC = Faction.CODEC.listOf().xmap(FactionData::new, data -> List.copyOf(data.factions.values()));
	/**
	 * Vanilla requires a data fixer type; the command storage one makes no changes this data
	 * would notice.
	 */
	public static final SavedDataType<FactionData> TYPE = new SavedDataType<>(
			CrownOfChaos.id("factions"), FactionData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Faction> factions = new LinkedHashMap<>();

	public FactionData() {
	}

	private FactionData(List<Faction> factions) {
		factions.forEach(faction -> this.factions.put(faction.id(), faction));
	}

	public static FactionData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public Collection<Faction> all() {
		return this.factions.values();
	}

	public @Nullable Faction get(UUID id) {
		return this.factions.get(id);
	}

	void add(Faction faction) {
		this.factions.put(faction.id(), faction);
		this.setDirty();
	}

	void remove(Faction faction) {
		this.factions.remove(faction.id());
		for (Faction other : this.factions.values()) {
			other.setStance(faction.id(), FactionStance.NEUTRAL);
			other.warFactions().remove(faction.id());
		}
		this.setDirty();
	}

	/** The faction the player belongs to, if any. A player is in at most one. */
	public Optional<Faction> factionOf(UUID player) {
		return this.factions.values().stream().filter(f -> f.isMember(player)).findFirst();
	}

	public Optional<Faction> byName(String name) {
		return this.factions.values().stream().filter(f -> f.name().equalsIgnoreCase(name)).findFirst();
	}

	public Optional<Faction> byColor(DyeColor color) {
		return this.factions.values().stream().filter(f -> f.color() == color).findFirst();
	}

	/** The faction whose territory covers this spot, if any. */
	public Optional<Faction> territoryAt(ResourceKey<Level> dimension, double x, double z) {
		for (Faction faction : this.factions.values()) {
			for (FactionFlag flag : faction.flags()) {
				if (flag.covers(dimension, x, z, FactionManager.CLAIM_RADIUS)) {
					return Optional.of(faction);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * A faction other than {@code placer} with a flag close enough that a new flag here would
	 * overlap its territory, if any.
	 */
	public Optional<Faction> overlappingFaction(ResourceKey<Level> dimension, double x, double z, UUID placer) {
		for (Faction faction : this.factions.values()) {
			if (faction.id().equals(placer)) {
				continue;
			}
			for (FactionFlag flag : faction.flags()) {
				if (flag.covers(dimension, x, z, FactionManager.CLAIM_RADIUS * 2)) {
					return Optional.of(faction);
				}
			}
		}
		return Optional.empty();
	}

	/** Whether any faction has a flag at or right next to this block (within 1 in every direction). */
	public boolean hasFlagNear(ResourceKey<Level> dimension, BlockPos pos) {
		for (Faction faction : this.factions.values()) {
			for (FactionFlag flag : faction.flags()) {
				if (flag.dimension() == dimension && flag.pos().distChessboard(pos) <= 1) {
					return true;
				}
			}
		}
		return false;
	}

	/** The faction with a flag at exactly this spot, if any. */
	public Optional<Faction> flagOwner(FactionFlag flag) {
		return this.factions.values().stream().filter(f -> f.flags().contains(flag)).findFirst();
	}
}

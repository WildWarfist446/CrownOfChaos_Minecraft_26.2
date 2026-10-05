package com.crownofchaos.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.DyeColor;

/**
 * A faction: its name (emoji allowed), its dye color (no two factions share one), its owner and
 * members, pending invites and join requests, its stance toward other factions, whether it's on
 * red alert, who it's at war with, and its placed flags. Player names are remembered so they can be shown while the
 * player is offline.
 */
public final class Faction {
	/** A player and the name they had when last seen. */
	public record Member(UUID id, String name) {
		public static final Codec<Member> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Member::id),
				Codec.STRING.fieldOf("name").forGetter(Member::name)
		).apply(instance, Member::new));
	}

	private record StanceEntry(UUID faction, FactionStance stance) {
		private static final Codec<StanceEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("faction").forGetter(StanceEntry::faction),
				FactionStance.CODEC.fieldOf("stance").forGetter(StanceEntry::stance)
		).apply(instance, StanceEntry::new));
	}

	public static final Codec<Faction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(f -> f.id),
			Codec.STRING.fieldOf("name").forGetter(f -> f.name),
			DyeColor.CODEC.fieldOf("color").forGetter(f -> f.color),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(f -> f.owner),
			Member.CODEC.listOf().fieldOf("members").forGetter(f -> toList(f.members)),
			Member.CODEC.listOf().optionalFieldOf("invites", List.of()).forGetter(f -> toList(f.invites)),
			Member.CODEC.listOf().optionalFieldOf("requests", List.of()).forGetter(f -> toList(f.requests)),
			Codec.BOOL.optionalFieldOf("red_alert", false).forGetter(f -> f.redAlert),
			StanceEntry.CODEC.listOf().optionalFieldOf("stances", List.of()).forGetter(Faction::stanceList),
			FactionFlag.CODEC.listOf().optionalFieldOf("flags", List.of()).forGetter(f -> f.flags),
			Codec.INT.optionalFieldOf("hidden_flag_count").forGetter(f -> f.hiddenFlagCount),
			UUIDUtil.CODEC.listOf().optionalFieldOf("war_factions", List.of()).forGetter(f -> List.copyOf(f.warFactions)),
			Member.CODEC.listOf().optionalFieldOf("war_players", List.of()).forGetter(f -> toList(f.warPlayers))
	).apply(instance, Faction::new));

	private final UUID id;
	private String name;
	private DyeColor color;
	private UUID owner;
	private final Map<UUID, String> members = new LinkedHashMap<>();
	private final Map<UUID, String> invites = new LinkedHashMap<>();
	private final Map<UUID, String> requests = new LinkedHashMap<>();
	private boolean redAlert;
	private final Map<UUID, FactionStance> stances = new HashMap<>();
	private final List<FactionFlag> flags = new ArrayList<>();
	/** Factions this faction is at war with: its units attack all their members and units, anywhere. */
	private final Set<UUID> warFactions = new LinkedHashSet<>();
	/**
	 * Players this faction is at war with on their own (not their whole faction), and their names:
	 * its units attack them and the units they own, anywhere.
	 */
	private final Map<UUID, String> warPlayers = new LinkedHashMap<>();
	/** On players' copies, how many flags the faction has; they aren't sent where the flags are. */
	private final Optional<Integer> hiddenFlagCount;

	public Faction(UUID id, String name, DyeColor color, Member owner) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.owner = owner.id();
		this.members.put(owner.id(), owner.name());
		this.hiddenFlagCount = Optional.empty();
	}

	private Faction(UUID id, String name, DyeColor color, UUID owner, List<Member> members, List<Member> invites, List<Member> requests,
			boolean redAlert, List<StanceEntry> stances, List<FactionFlag> flags, Optional<Integer> hiddenFlagCount,
			List<UUID> warFactions, List<Member> warPlayers) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.owner = owner;
		members.forEach(m -> this.members.put(m.id(), m.name()));
		invites.forEach(m -> this.invites.put(m.id(), m.name()));
		requests.forEach(m -> this.requests.put(m.id(), m.name()));
		this.redAlert = redAlert;
		stances.forEach(s -> this.stances.put(s.faction(), s.stance()));
		this.flags.addAll(flags);
		this.hiddenFlagCount = hiddenFlagCount;
		this.warFactions.addAll(warFactions);
		warPlayers.forEach(m -> this.warPlayers.put(m.id(), m.name()));
	}

	/** A copy for sending to players: everything except where the flags are. */
	public Faction withoutFlagPositions() {
		return new Faction(this.id, this.name, this.color, this.owner, toList(this.members), toList(this.invites), toList(this.requests),
				this.redAlert, this.stanceList(), List.of(), Optional.of(this.flags.size()), List.copyOf(this.warFactions), toList(this.warPlayers));
	}

	private static List<Member> toList(Map<UUID, String> players) {
		return players.entrySet().stream().map(e -> new Member(e.getKey(), e.getValue())).toList();
	}

	private List<StanceEntry> stanceList() {
		return this.stances.entrySet().stream().map(e -> new StanceEntry(e.getKey(), e.getValue())).toList();
	}

	public UUID id() {
		return this.id;
	}

	public String name() {
		return this.name;
	}

	void setName(String name) {
		this.name = name;
	}

	public DyeColor color() {
		return this.color;
	}

	void setColor(DyeColor color) {
		this.color = color;
	}

	/** The faction's name in its color. */
	public MutableComponent displayName() {
		return Component.literal(this.name).withColor(this.color.getTextColor());
	}

	public UUID owner() {
		return this.owner;
	}

	void setOwner(UUID owner) {
		this.owner = owner;
	}

	public boolean isOwner(UUID player) {
		return this.owner.equals(player);
	}

	/** Members (including the owner), and the names they had when last seen. */
	public Map<UUID, String> members() {
		return this.members;
	}

	public boolean isMember(UUID player) {
		return this.members.containsKey(player);
	}

	/** Players the owner has invited to join. */
	public Map<UUID, String> invites() {
		return this.invites;
	}

	/** Players asking to join, waiting for the owner. */
	public Map<UUID, String> requests() {
		return this.requests;
	}

	public boolean redAlert() {
		return this.redAlert;
	}

	void setRedAlert(boolean redAlert) {
		this.redAlert = redAlert;
	}

	/** This faction's stance toward another faction: neutral unless the owner chose otherwise. */
	public FactionStance stanceToward(UUID otherFaction) {
		return this.stances.getOrDefault(otherFaction, FactionStance.NEUTRAL);
	}

	void setStance(UUID otherFaction, FactionStance stance) {
		if (stance == FactionStance.NEUTRAL) {
			this.stances.remove(otherFaction);
		} else {
			this.stances.put(otherFaction, stance);
		}
	}

	/** Factions this faction is at war with (all their members and units are enemies, anywhere). */
	public Set<UUID> warFactions() {
		return this.warFactions;
	}

	/** Players this faction is at war with on their own, and their names (they and their units are enemies). */
	public Map<UUID, String> warPlayers() {
		return this.warPlayers;
	}

	/** Whether this faction is at war with anyone, which stops its members reclaiming units. */
	public boolean atWar() {
		return !this.warFactions.isEmpty() || !this.warPlayers.isEmpty();
	}

	/** Placed flags (server only; empty on players' copies). */
	public List<FactionFlag> flags() {
		return this.flags;
	}

	public int flagCount() {
		return this.hiddenFlagCount.orElse(this.flags.size());
	}
}

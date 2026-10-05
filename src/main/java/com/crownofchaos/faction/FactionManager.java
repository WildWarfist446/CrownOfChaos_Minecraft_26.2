package com.crownofchaos.faction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.item.FactionFlagItem;
import com.crownofchaos.network.FactionActionPayload;
import com.crownofchaos.network.FactionSyncPayload;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Server-side faction logic: handles what players do in the faction menu, keeps each faction's
 * scoreboard team (the colored {@code [Faction]} tag before members' names in chat and above
 * their heads) in step, sends every player the factions whenever they change, shows a title when
 * players enter a faction's territory (and a note when they leave it), and keeps track of placed flags.
 */
public final class FactionManager {
	/** A flag claims every spot within this many blocks across the ground, at any height. */
	public static final int CLAIM_RADIUS = 100;
	/** Longest faction name, counting each emoji as one character. */
	public static final int MAX_NAME_LENGTH = 24;
	/** Scoreboard teams for factions are named this plus part of the faction's id. */
	private static final String TEAM_PREFIX = "coc_faction_";
	/** How often to check which territory each player is in: twice a second. */
	private static final int TERRITORY_CHECK_TICKS = 10;
	/** How often to check that every placed flag is still there: every 5 seconds. */
	private static final int FLAG_CHECK_TICKS = 100;

	/** The territory title fades in over half a second, stays 2.5 seconds, and fades out over 0.75. */
	private static final int TITLE_FADE_IN_TICKS = 10;
	private static final int TITLE_STAY_TICKS = 50;
	private static final int TITLE_FADE_OUT_TICKS = 15;

	private static final ChatFormatting INFO = ChatFormatting.GOLD;
	private static final ChatFormatting ERROR = ChatFormatting.RED;

	/** The faction territory each player was last in (null for none). Only touched on the server thread. */
	private static final Map<UUID, UUID> LAST_TERRITORY = new HashMap<>();

	private static @Nullable MinecraftServer server;

	private FactionManager() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(FactionActionPayload.TYPE, FactionActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FactionSyncPayload.TYPE, FactionSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(FactionActionPayload.TYPE, (payload, context) -> handle(context.player(), payload));

		ServerLifecycleEvents.SERVER_STARTED.register(started -> {
			server = started;
			updateAllTeams();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
			server = null;
			LAST_TERRITORY.clear();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, joined) -> {
			ServerPlayer player = handler.getPlayer();
			FactionData data = FactionData.get(joined);
			// Remember the player's current name, in case they changed it.
			for (Faction faction : data.all()) {
				for (Map<UUID, String> players : List.of(faction.members(), faction.invites(), faction.requests())) {
					players.computeIfPresent(player.getUUID(), (id, oldName) -> {
						data.setDirty();
						return player.getScoreboardName();
					});
				}
			}
			data.factionOf(player.getUUID()).ifPresent(FactionManager::updateTeam);
			sendAll();
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, stopped) -> LAST_TERRITORY.remove(handler.getPlayer().getUUID()));

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel) {
				return !breakFlag(serverLevel, pos, player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
			}
			return true;
		});

		ServerTickEvents.END_SERVER_TICK.register(ticking -> {
			int tick = ticking.getTickCount();
			if (tick % TERRITORY_CHECK_TICKS == 0) {
				checkTerritories(ticking);
			}
			if (tick % FLAG_CHECK_TICKS == 0) {
				checkFlags(ticking);
			}
		});
	}

	// Actions from the faction menu

	private static void handle(ServerPlayer player, FactionActionPayload payload) {
		MinecraftServer server = player.level().getServer();
		FactionData data = FactionData.get(server);
		Optional<Faction> mine = data.factionOf(player.getUUID());
		Optional<UUID> target = payload.target();

		switch (payload.action()) {
			case CREATE -> create(player, data, payload.text(), payload.number());
			case RENAME -> ifOwner(player, mine, faction -> rename(player, data, faction, payload.text()));
			case SET_COLOR -> ifOwner(player, mine, faction -> setColor(player, data, faction, payload.number()));
			case DISBAND -> ifOwner(player, mine, faction -> disband(server, data, faction));
			case LEAVE -> leave(player, data, mine);
			case INVITE -> ifOwner(player, mine, faction -> invite(player, server, data, faction, payload.text()));
			case CANCEL_INVITE -> ifOwner(player, mine, faction -> target.ifPresent(id -> {
				String name = faction.invites().remove(id);
				if (name != null) {
					reply(player, Component.translatable("message.crownofchaos.faction.invite_cancelled", name), INFO);
				}
			}));
			case ACCEPT_INVITE -> target.map(data::get).ifPresent(faction -> {
				if (mine.isPresent()) {
					reply(player, Component.translatable("message.crownofchaos.faction.already_in_faction"), ERROR);
				} else if (faction.invites().containsKey(player.getUUID())) {
					join(server, data, faction, player.getUUID(), player.getScoreboardName());
				}
			});
			case DECLINE_INVITE -> target.map(data::get).ifPresent(faction -> {
				if (faction.invites().remove(player.getUUID()) != null) {
					reply(player, Component.translatable("message.crownofchaos.faction.invite_declined", faction.displayName()), INFO);
					notifyOwner(server, faction, Component.translatable("message.crownofchaos.faction.invite_declined_owner", player.getScoreboardName()));
				}
			});
			case REQUEST_JOIN -> target.map(data::get).ifPresent(faction -> requestJoin(player, server, data, mine, faction));
			case CANCEL_REQUEST -> target.map(data::get).ifPresent(faction -> {
				if (faction.requests().remove(player.getUUID()) != null) {
					reply(player, Component.translatable("message.crownofchaos.faction.request_cancelled", faction.displayName()), INFO);
				}
			});
			case ACCEPT_REQUEST -> ifOwner(player, mine, faction -> target.ifPresent(id -> {
				String name = faction.requests().remove(id);
				if (name == null) {
					return;
				}
				if (data.factionOf(id).isPresent()) {
					reply(player, Component.translatable("message.crownofchaos.faction.player_in_other_faction", name), ERROR);
				} else {
					join(server, data, faction, id, name);
				}
			}));
			case DECLINE_REQUEST -> ifOwner(player, mine, faction -> target.ifPresent(id -> {
				String name = faction.requests().remove(id);
				if (name != null) {
					reply(player, Component.translatable("message.crownofchaos.faction.request_declined_owner", name), INFO);
					tell(server, id, Component.translatable("message.crownofchaos.faction.request_declined", faction.displayName()), INFO);
				}
			}));
			case KICK -> ifOwner(player, mine, faction -> target.ifPresent(id -> {
				if (!id.equals(faction.owner()) && faction.isMember(id)) {
					String name = faction.members().get(id);
					removeMember(server, faction, id);
					broadcast(server, faction, Component.translatable("message.crownofchaos.faction.kicked", name));
					tell(server, id, Component.translatable("message.crownofchaos.faction.you_were_kicked", faction.displayName()), ERROR);
				}
			}));
			case TRANSFER -> ifOwner(player, mine, faction -> target.ifPresent(id -> {
				if (!id.equals(faction.owner()) && faction.isMember(id)) {
					faction.setOwner(id);
					broadcast(server, faction, Component.translatable("message.crownofchaos.faction.new_owner", faction.members().get(id)));
				}
			}));
			case SET_RED_ALERT -> ifOwner(player, mine, faction -> {
				faction.setRedAlert(payload.number() != 0);
				broadcast(server, faction, Component.translatable(faction.redAlert()
						? "message.crownofchaos.faction.red_alert_on" : "message.crownofchaos.faction.red_alert_off"));
			});
			case SET_STANCE -> ifOwner(player, mine, faction -> target.map(data::get).ifPresent(other -> {
				if (other != faction && payload.number() >= 0 && payload.number() < FactionStance.values().length) {
					FactionStance stance = FactionStance.values()[payload.number()];
					faction.setStance(other.id(), stance);
					reply(player, Component.translatable("message.crownofchaos.faction.stance." + stance.getSerializedName(), other.displayName()), INFO);
				}
			}));
		}

		data.setDirty();
		sendAll();
	}

	private interface FactionAction {
		void run(Faction faction);
	}

	/** Runs the action if the player owns a faction; otherwise says why not. */
	private static void ifOwner(ServerPlayer player, Optional<Faction> mine, FactionAction action) {
		if (mine.isEmpty()) {
			reply(player, Component.translatable("message.crownofchaos.faction.not_in_faction"), ERROR);
		} else if (!mine.get().isOwner(player.getUUID())) {
			reply(player, Component.translatable("message.crownofchaos.faction.not_owner"), ERROR);
		} else {
			action.run(mine.get());
		}
	}

	private static void create(ServerPlayer player, FactionData data, String rawName, int colorId) {
		if (data.factionOf(player.getUUID()).isPresent()) {
			reply(player, Component.translatable("message.crownofchaos.faction.already_in_faction"), ERROR);
			return;
		}
		String name = checkName(player, data, rawName, null);
		DyeColor color = checkColor(player, data, colorId, null);
		if (name == null || color == null) {
			return;
		}

		Faction faction = new Faction(UUID.randomUUID(), name, color, new Faction.Member(player.getUUID(), player.getScoreboardName()));
		clearInvitesAndRequests(data, player.getUUID());
		data.add(faction);
		updateTeam(faction);
		reply(player, Component.translatable("message.crownofchaos.faction.created", faction.displayName()), INFO);
	}

	private static void rename(ServerPlayer player, FactionData data, Faction faction, String rawName) {
		String name = checkName(player, data, rawName, faction);
		if (name != null) {
			faction.setName(name);
			updateTeam(faction);
			broadcast(player.level().getServer(), faction, Component.translatable("message.crownofchaos.faction.renamed", faction.displayName()));
		}
	}

	private static void setColor(ServerPlayer player, FactionData data, Faction faction, int colorId) {
		DyeColor color = checkColor(player, data, colorId, faction);
		if (color != null) {
			faction.setColor(color);
			updateTeam(faction);
			broadcast(player.level().getServer(), faction, Component.translatable("message.crownofchaos.faction.recolored", faction.displayName()));
		}
	}

	/**
	 * The cleaned-up name, or null (after telling the player why) if it's empty, too long, uses
	 * characters chat doesn't allow, or another faction has it.
	 */
	private static @Nullable String checkName(ServerPlayer player, FactionData data, String rawName, @Nullable Faction renaming) {
		String name = rawName.strip();
		int length = name.codePointCount(0, name.length());
		if (length == 0 || length > MAX_NAME_LENGTH || !name.codePoints().allMatch(StringUtil::isAllowedChatCharacter)) {
			reply(player, Component.translatable("message.crownofchaos.faction.bad_name", MAX_NAME_LENGTH), ERROR);
			return null;
		}
		Optional<Faction> existing = data.byName(name);
		if (existing.isPresent() && existing.get() != renaming) {
			reply(player, Component.translatable("message.crownofchaos.faction.name_taken", name), ERROR);
			return null;
		}
		return name;
	}

	/** The dye color, or null (after telling the player why) if there's no such color or another faction has it. */
	private static @Nullable DyeColor checkColor(ServerPlayer player, FactionData data, int colorId, @Nullable Faction recoloring) {
		if (colorId < 0 || colorId >= DyeColor.values().length) {
			return null;
		}
		DyeColor color = DyeColor.byId(colorId);
		Optional<Faction> existing = data.byColor(color);
		if (existing.isPresent() && existing.get() != recoloring) {
			reply(player, Component.translatable("message.crownofchaos.faction.color_taken", existing.get().displayName()), ERROR);
			return null;
		}
		return color;
	}

	private static void disband(MinecraftServer server, FactionData data, Faction faction) {
		broadcast(server, faction, Component.translatable("message.crownofchaos.faction.disbanded", faction.displayName()));
		ServerScoreboard scoreboard = server.getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam(teamName(faction));
		if (team != null) {
			scoreboard.removePlayerTeam(team);
		}
		data.remove(faction);
	}

	private static void leave(ServerPlayer player, FactionData data, Optional<Faction> mine) {
		if (mine.isEmpty()) {
			reply(player, Component.translatable("message.crownofchaos.faction.not_in_faction"), ERROR);
		} else if (mine.get().isOwner(player.getUUID())) {
			reply(player, Component.translatable("message.crownofchaos.faction.owner_cant_leave"), ERROR);
		} else {
			Faction faction = mine.get();
			removeMember(player.level().getServer(), faction, player.getUUID());
			reply(player, Component.translatable("message.crownofchaos.faction.left", faction.displayName()), INFO);
			broadcast(player.level().getServer(), faction, Component.translatable("message.crownofchaos.faction.member_left", player.getScoreboardName()));
		}
	}

	private static void invite(ServerPlayer player, MinecraftServer server, FactionData data, Faction faction, String name) {
		ServerPlayer invited = server.getPlayerList().getPlayerByName(name.strip());
		if (invited == null) {
			reply(player, Component.translatable("message.crownofchaos.faction.player_not_found", name.strip()), ERROR);
		} else if (data.factionOf(invited.getUUID()).isPresent()) {
			reply(player, Component.translatable("message.crownofchaos.faction.player_in_other_faction", invited.getScoreboardName()), ERROR);
		} else if (faction.requests().containsKey(invited.getUUID())) {
			// They already asked to join, so the invite lets them straight in.
			faction.requests().remove(invited.getUUID());
			join(server, data, faction, invited.getUUID(), invited.getScoreboardName());
		} else if (faction.invites().putIfAbsent(invited.getUUID(), invited.getScoreboardName()) == null) {
			reply(player, Component.translatable("message.crownofchaos.faction.invited", invited.getScoreboardName()), INFO);
			reply(invited, Component.translatable("message.crownofchaos.faction.you_were_invited", faction.displayName()), INFO);
		}
	}

	private static void requestJoin(ServerPlayer player, MinecraftServer server, FactionData data, Optional<Faction> mine, Faction faction) {
		if (mine.isPresent()) {
			reply(player, Component.translatable("message.crownofchaos.faction.already_in_faction"), ERROR);
		} else if (faction.invites().containsKey(player.getUUID())) {
			// Already invited, so asking is as good as accepting.
			join(server, data, faction, player.getUUID(), player.getScoreboardName());
		} else if (faction.requests().putIfAbsent(player.getUUID(), player.getScoreboardName()) == null) {
			reply(player, Component.translatable("message.crownofchaos.faction.requested", faction.displayName()), INFO);
			notifyOwner(server, faction, Component.translatable("message.crownofchaos.faction.request_received", player.getScoreboardName()));
		}
	}

	private static void join(MinecraftServer server, FactionData data, Faction faction, UUID player, String name) {
		clearInvitesAndRequests(data, player);
		faction.members().put(player, name);
		updateTeam(faction);
		broadcast(server, faction, Component.translatable("message.crownofchaos.faction.joined", name, faction.displayName()));
	}

	private static void removeMember(MinecraftServer server, Faction faction, UUID player) {
		String name = faction.members().remove(player);
		if (name != null) {
			PlayerTeam team = server.getScoreboard().getPlayersTeam(name);
			if (team != null && team.getName().equals(teamName(faction))) {
				server.getScoreboard().removePlayerFromTeam(name, team);
			}
		}
	}

	/** Once a player is in a faction, their other invites and requests no longer apply. */
	private static void clearInvitesAndRequests(FactionData data, UUID player) {
		for (Faction faction : data.all()) {
			faction.invites().remove(player);
			faction.requests().remove(player);
		}
	}

	// Name tags

	private static String teamName(Faction faction) {
		return TEAM_PREFIX + faction.id().toString().substring(0, 8);
	}

	/** Makes the faction's scoreboard team match it: the colored name tag and who's in it. */
	private static void updateTeam(Faction faction) {
		if (server == null) {
			return;
		}
		ServerScoreboard scoreboard = server.getScoreboard();
		String name = teamName(faction);
		PlayerTeam team = scoreboard.getPlayerTeam(name);
		if (team == null) {
			team = scoreboard.addPlayerTeam(name);
		}
		team.setDisplayName(faction.displayName());
		team.setPlayerPrefix(Component.literal("[" + faction.name() + "] ").withColor(faction.color().getTextColor()));
		for (String member : faction.members().values()) {
			if (scoreboard.getPlayersTeam(member) != team) {
				scoreboard.addPlayerToTeam(member, team);
			}
		}
		for (String entry : List.copyOf(team.getPlayers())) {
			if (!faction.members().containsValue(entry)) {
				scoreboard.removePlayerFromTeam(entry, team);
			}
		}
	}

	/** On server start: updates every faction's team and removes teams of factions that are gone. */
	private static void updateAllTeams() {
		if (server == null) {
			return;
		}
		FactionData data = FactionData.get(server);
		ServerScoreboard scoreboard = server.getScoreboard();
		for (PlayerTeam team : List.copyOf(scoreboard.getPlayerTeams())) {
			if (team.getName().startsWith(TEAM_PREFIX) && data.all().stream().noneMatch(f -> teamName(f).equals(team.getName()))) {
				scoreboard.removePlayerTeam(team);
			}
		}
		data.all().forEach(FactionManager::updateTeam);
	}

	// Syncing and messages

	/** Sends every player all factions, without where their flags are. */
	private static void sendAll() {
		if (server == null) {
			return;
		}
		FactionSyncPayload payload = new FactionSyncPayload(
				FactionData.get(server).all().stream().map(Faction::withoutFlagPositions).toList());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// Players without the mod can't receive it.
			if (ServerPlayNetworking.canSend(player, FactionSyncPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private static void reply(ServerPlayer player, MutableComponent message, ChatFormatting color) {
		player.sendSystemMessage(message.withStyle(color));
	}

	private static void tell(MinecraftServer server, UUID player, MutableComponent message, ChatFormatting color) {
		ServerPlayer online = server.getPlayerList().getPlayer(player);
		if (online != null) {
			reply(online, message, color);
		}
	}

	/** Tells every online member of the faction. */
	private static void broadcast(MinecraftServer server, Faction faction, MutableComponent message) {
		for (UUID member : faction.members().keySet()) {
			tell(server, member, message.copy(), INFO);
		}
	}

	private static void notifyOwner(MinecraftServer server, Faction faction, MutableComponent message) {
		tell(server, faction.owner(), message, INFO);
	}

	// Territory and flags

	/** Tells players in the action bar when they cross into or out of a faction's territory. */
	private static void checkTerritories(MinecraftServer server) {
		FactionData data = FactionData.get(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Faction here = data.territoryAt(player.level().dimension(), player.getX(), player.getZ()).orElse(null);
			UUID hereId = here == null ? null : here.id();
			UUID before = LAST_TERRITORY.containsKey(player.getUUID()) ? LAST_TERRITORY.get(player.getUUID()) : hereId;
			LAST_TERRITORY.put(player.getUUID(), hereId);
			if (Objects.equals(before, hereId)) {
				continue;
			}
			if (here != null) {
				showTerritoryTitle(player, here);
			} else {
				Faction left = data.get(before);
				player.sendOverlayMessage(left == null
						? Component.translatable("message.crownofchaos.faction.wilderness").withStyle(ChatFormatting.GRAY)
						: Component.translatable("message.crownofchaos.faction.leaving", left.displayName()));
			}
		}
	}

	/**
	 * The big on-screen title for entering a faction's territory: its name in its color, with
	 * "Faction Territory" (or "Your Faction's Territory") underneath.
	 */
	private static void showTerritoryTitle(ServerPlayer player, Faction faction) {
		String subtitle = faction.isMember(player.getUUID())
				? "message.crownofchaos.faction.own_territory_subtitle" : "message.crownofchaos.faction.territory_subtitle";
		player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(subtitle).withStyle(ChatFormatting.GRAY)));
		player.connection.send(new ClientboundSetTitleTextPacket(faction.displayName()));
	}

	/** Places a flag for the player's faction, after {@link FactionFlagItem} has checked the spot. */
	public static void addFlag(ServerPlayer player, Faction faction, FactionFlag flag) {
		FactionData data = FactionData.get(player.level().getServer());
		faction.flags().add(flag);
		data.setDirty();
		broadcast(player.level().getServer(), faction, Component.translatable("message.crownofchaos.faction.flag_placed",
				player.getScoreboardName(), flag.pos().getX(), flag.pos().getY(), flag.pos().getZ()));
		sendAll();
	}

	/**
	 * If there's a faction flag here, removes its claim, breaks it and drops it as a Faction Flag
	 * of the same faction (not in creative), and returns true. Anyone can break a flag.
	 */
	private static boolean breakFlag(ServerLevel level, BlockPos pos, @Nullable ServerPlayer breaker) {
		FactionData data = FactionData.get(level.getServer());
		FactionFlag flag = new FactionFlag(level.dimension(), pos.immutable());
		Optional<Faction> owner = data.flagOwner(flag);
		if (owner.isEmpty()) {
			return false;
		}
		Faction faction = owner.get();
		removeFlag(level.getServer(), faction, flag, breaker == null ? null : breaker.getScoreboardName());
		level.removeBlock(pos, false);
		if (breaker == null || !breaker.isCreative()) {
			Block.popResource(level, pos, FactionFlagItem.forFaction(faction));
		}
		return true;
	}

	private static void removeFlag(MinecraftServer server, Faction faction, FactionFlag flag, @Nullable String breaker) {
		faction.flags().remove(flag);
		FactionData.get(server).setDirty();
		Component message = breaker == null
				? Component.translatable("message.crownofchaos.faction.flag_lost", flag.pos().getX(), flag.pos().getY(), flag.pos().getZ())
				: Component.translatable("message.crownofchaos.faction.flag_broken", breaker, flag.pos().getX(), flag.pos().getY(), flag.pos().getZ());
		broadcast(server, faction, message.copy());
		sendAll();
	}

	/**
	 * Removes the claim of any flag that's no longer there: blown up, pushed by a piston, or
	 * dropped because the block it hung on was broken. Only checks flags in loaded chunks.
	 */
	private static void checkFlags(MinecraftServer server) {
		FactionData data = FactionData.get(server);
		for (Faction faction : List.copyOf(data.all())) {
			Iterator<FactionFlag> flags = List.copyOf(faction.flags()).iterator();
			while (flags.hasNext()) {
				FactionFlag flag = flags.next();
				ServerLevel level = server.getLevel(flag.dimension());
				if (level != null && level.isLoaded(flag.pos())
						&& !(level.getBlockState(flag.pos()).getBlock() instanceof AbstractBannerBlock)) {
					removeFlag(server, faction, flag, null);
				}
			}
		}
	}

	/** The faction that owns the territory at the player's position, if any. Used by other systems. */
	public static Optional<Faction> territoryAt(Level level, double x, double z) {
		return level.getServer() == null ? Optional.empty() : FactionData.get(level.getServer()).territoryAt(level.dimension(), x, z);
	}
}

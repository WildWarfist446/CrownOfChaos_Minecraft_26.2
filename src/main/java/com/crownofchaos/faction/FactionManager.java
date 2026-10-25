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
import com.crownofchaos.unit.UnitManager;
import com.crownofchaos.network.FactionActionPayload;
import com.crownofchaos.network.FactionSyncPayload;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
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

	/** Whether players in creative mode (such as server admins) can break any faction's flags. */
	private static final boolean CREATIVE_BREAKS_ANY_FLAG = true;

	private static final ChatFormatting INFO = ChatFormatting.GOLD;
	private static final ChatFormatting ERROR = ChatFormatting.RED;

	/** The faction territory each player was last in (null for none). Only touched on the server thread. */
	private static final Map<UUID, UUID> LAST_TERRITORY = new HashMap<>();

	/** Players who just joined, and the tick at which they are sent all the factions a second time, in case the first was missed. */
	private static final Map<UUID, Integer> RESYNC_AT = new HashMap<>();
	private static final int RESYNC_DELAY_TICKS = 40;

	private static @Nullable MinecraftServer server;

	private FactionManager() {
	}

	public static void initialize() {
		FlagLabels.initialize();
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
			RESYNC_AT.clear();
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
			// Again a moment later, once the player's game has surely finished joining.
			RESYNC_AT.put(player.getUUID(), joined.getTickCount() + RESYNC_DELAY_TICKS);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, stopped) -> {
			LAST_TERRITORY.remove(handler.getPlayer().getUUID());
			RESYNC_AT.remove(handler.getPlayer().getUUID());
		});

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
				return true;
			}
			Optional<Faction> protector = protectingFaction(serverLevel, pos);
			if (protector.isPresent() && !protector.get().isMember(serverPlayer.getUUID())
					&& !(CREATIVE_BREAKS_ANY_FLAG && serverPlayer.isCreative())) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.crownofchaos.faction.flag_protected",
						protector.get().displayName()).withStyle(ERROR));
				return false;
			}
			return !breakFlag(serverLevel, pos, serverPlayer);
		});

		ServerTickEvents.END_SERVER_TICK.register(ticking -> {
			int tick = ticking.getTickCount();
			if (!RESYNC_AT.isEmpty()) {
				RESYNC_AT.entrySet().removeIf(entry -> {
					if (tick < entry.getValue()) {
						return false;
					}
					ServerPlayer joinedPlayer = ticking.getPlayerList().getPlayer(entry.getKey());
					if (joinedPlayer != null) {
						sendTo(joinedPlayer);
					}
					return true;
				});
			}
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
		if (payload.action() == FactionActionPayload.Action.SYNC) {
			sendTo(player);
			return;
		}
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
			case DECLARE_WAR -> ifOwner(player, mine, faction -> target.map(data::get).ifPresent(other -> {
				if (other != faction && faction.warFactions().add(other.id())) {
					broadcast(server, faction, Component.translatable("message.crownofchaos.faction.war_declared", other.displayName()));
					warn(server, other, Component.translatable("message.crownofchaos.faction.war_declared_on_you", faction.displayName()));
				}
			}));
			case END_WAR -> ifOwner(player, mine, faction -> target.map(data::get).ifPresent(other -> {
				if (faction.warFactions().remove(other.id())) {
					broadcast(server, faction, Component.translatable("message.crownofchaos.faction.peace_made", other.displayName()));
					broadcast(server, other, Component.translatable("message.crownofchaos.faction.peace_made_with_you", faction.displayName()));
				}
			}));
			case DECLARE_WAR_ON_PLAYER -> ifOwner(player, mine, faction -> declareWarOnPlayer(player, server, data, faction, payload.text()));
			case END_WAR_ON_PLAYER -> ifOwner(player, mine, faction -> target.ifPresent(id -> {
				String name = faction.warPlayers().remove(id);
				if (name != null) {
					broadcast(server, faction, Component.translatable("message.crownofchaos.faction.peace_made", name));
					tell(server, id, Component.translatable("message.crownofchaos.faction.peace_made_with_you", faction.displayName()), INFO);
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
			updateLabels(player.level().getServer(), faction);
			broadcast(player.level().getServer(), faction, Component.translatable("message.crownofchaos.faction.renamed", faction.displayName()));
		}
	}

	private static void setColor(ServerPlayer player, FactionData data, Faction faction, int colorId) {
		DyeColor color = checkColor(player, data, colorId, faction);
		if (color != null) {
			faction.setColor(color);
			updateTeam(faction);
			updateLabels(player.level().getServer(), faction);
			UnitManager.recolor(player.level().getServer(), faction);
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
		// The banners stay as decoration, but stop being flags.
		faction.flags().forEach(flag -> removeLabel(server, flag));
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

	/**
	 * Declares war on one player alone: an online player, or a known member of any faction. Your
	 * units attack them and their units, but not the rest of their faction.
	 */
	private static void declareWarOnPlayer(ServerPlayer player, MinecraftServer server, FactionData data, Faction faction, String rawName) {
		String name = rawName.strip();
		UUID id = null;
		String knownName = name;
		ServerPlayer online = server.getPlayerList().getPlayerByName(name);
		if (online != null) {
			id = online.getUUID();
			knownName = online.getScoreboardName();
		} else {
			for (Faction other : data.all()) {
				for (Map.Entry<UUID, String> member : other.members().entrySet()) {
					if (member.getValue().equalsIgnoreCase(name)) {
						id = member.getKey();
						knownName = member.getValue();
					}
				}
			}
		}
		if (id == null) {
			reply(player, Component.translatable("message.crownofchaos.faction.war_player_not_found", name), ERROR);
		} else if (faction.isMember(id)) {
			reply(player, Component.translatable("message.crownofchaos.faction.war_on_own_member"), ERROR);
		} else if (faction.warPlayers().putIfAbsent(id, knownName) == null) {
			broadcast(server, faction, Component.translatable("message.crownofchaos.faction.war_declared", knownName));
			tell(server, id, Component.translatable("message.crownofchaos.faction.war_declared_on_you", faction.displayName()), ERROR);
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
		FactionSyncPayload payload = syncPayload();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player, payload);
		}
	}

	/** Sends one player all the factions again (asked for by their game, and sent shortly after they join). */
	private static void sendTo(ServerPlayer player) {
		if (server != null) {
			send(player, syncPayload());
		}
	}

	private static FactionSyncPayload syncPayload() {
		return new FactionSyncPayload(FactionData.get(server).all().stream().map(Faction::withoutFlagPositions).toList());
	}

	private static void send(ServerPlayer player, FactionSyncPayload payload) {
		// Players without the mod can't receive it.
		if (ServerPlayNetworking.canSend(player, FactionSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
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

	/** Tells every online member of the faction, in red. */
	private static void warn(MinecraftServer server, Faction faction, MutableComponent message) {
		for (UUID member : faction.members().keySet()) {
			tell(server, member, message.copy(), ERROR);
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
		FlagLabels.update((ServerLevel) player.level(), faction, flag);
		data.setDirty();
		broadcast(player.level().getServer(), faction, Component.translatable("message.crownofchaos.faction.flag_placed",
				player.getScoreboardName(), flag.pos().getX(), flag.pos().getY(), flag.pos().getZ()));
		sendAll();
	}

	/**
	 * The faction whose flag is here, or whose flag stands on or hangs from the block here, if
	 * any. Only that faction's members can break it, so other players can't knock a flag down by
	 * breaking what holds it up.
	 */
	private static Optional<Faction> protectingFaction(ServerLevel level, BlockPos pos) {
		FactionData data = FactionData.get(level.getServer());
		// Quick check first, since explosions and pistons ask about many blocks.
		if (!data.hasFlagNear(level.dimension(), pos)) {
			return Optional.empty();
		}
		Optional<Faction> owner = data.flagOwner(new FactionFlag(level.dimension(), pos.immutable()));
		if (owner.isPresent()) {
			return owner;
		}
		for (Direction direction : Direction.values()) {
			BlockPos flagPos = pos.relative(direction);
			BlockState flagState = level.getBlockState(flagPos);
			if (!(flagState.getBlock() instanceof AbstractBannerBlock)) {
				continue;
			}
			BlockPos support = flagState.getBlock() instanceof WallBannerBlock
					? flagPos.relative(flagState.getValue(WallBannerBlock.FACING).getOpposite())
					: flagPos.below();
			if (support.equals(pos)) {
				Optional<Faction> flagOwner = data.flagOwner(new FactionFlag(level.dimension(), flagPos.immutable()));
				if (flagOwner.isPresent()) {
					return flagOwner;
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Whether the block here is a faction flag or holds one up, so explosions and pistons must
	 * leave it alone (see {@code ServerExplosionMixin} and {@code PistonStructureResolverMixin}).
	 */
	public static boolean isFlagProtected(ServerLevel level, BlockPos pos) {
		return protectingFaction(level, pos).isPresent();
	}

	/**
	 * If there's a faction flag here, removes its claim, breaks it and drops it as a Faction Flag
	 * of the same faction (not in creative), and returns true. Only members get this far (see
	 * {@link #protectingFaction}).
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
		// The flag keeps the patterns it was given at a loom.
		ItemStack dropped = FactionFlagItem.forFaction(faction);
		if (level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
			dropped.set(DataComponents.BANNER_PATTERNS, banner.getPatterns());
		}
		level.removeBlock(pos, false);
		if (breaker == null || !breaker.isCreative()) {
			Block.popResource(level, pos, dropped);
		}
		return true;
	}

	private static void removeFlag(MinecraftServer server, Faction faction, FactionFlag flag, @Nullable String breaker) {
		faction.flags().remove(flag);
		removeLabel(server, flag);
		FactionData.get(server).setDirty();
		Component message = breaker == null
				? Component.translatable("message.crownofchaos.faction.flag_lost", flag.pos().getX(), flag.pos().getY(), flag.pos().getZ())
				: Component.translatable("message.crownofchaos.faction.flag_broken", breaker, flag.pos().getX(), flag.pos().getY(), flag.pos().getZ());
		broadcast(server, faction, message.copy());
		sendAll();
	}

	/**
	 * Refreshes the faction's flags in loaded chunks after a rename or recolor: their banner color
	 * and name labels (the rest catch up when loaded, in {@link #checkFlags}).
	 */
	private static void updateLabels(MinecraftServer server, Faction faction) {
		for (FactionFlag flag : faction.flags()) {
			ServerLevel level = server.getLevel(flag.dimension());
			if (level != null && level.isLoaded(flag.pos())) {
				repaintFlag(level, flag, faction.color());
				FlagLabels.update(level, faction, flag);
			}
		}
	}

	/** Swaps a placed flag's banner for one in the faction's current color, keeping its angle or wall. */
	private static void repaintFlag(ServerLevel level, FactionFlag flag, DyeColor color) {
		BlockState state = level.getBlockState(flag.pos());
		if (!(state.getBlock() instanceof AbstractBannerBlock banner) || banner.getColor() == color) {
			return;
		}
		BlockState repainted;
		if (state.getBlock() instanceof WallBannerBlock) {
			repainted = bannerBlock(color, "_wall_banner").defaultBlockState().setValue(WallBannerBlock.FACING, state.getValue(WallBannerBlock.FACING));
		} else {
			repainted = bannerBlock(color, "_banner").defaultBlockState().setValue(BannerBlock.ROTATION, state.getValue(BannerBlock.ROTATION));
		}
		level.setBlockAndUpdate(flag.pos(), repainted);
	}

	private static Block bannerBlock(DyeColor color, String suffix) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + suffix));
	}

	private static void removeLabel(MinecraftServer server, FactionFlag flag) {
		ServerLevel level = server.getLevel(flag.dimension());
		if (level != null && level.isLoaded(flag.pos())) {
			FlagLabels.remove(level, flag);
		}
	}

	/**
	 * Removes the claim of any flag that's no longer there (removed by commands or other mods,
	 * since players outside the faction, explosions and pistons can't break flags), and keeps
	 * every remaining flag's name label up to date. Only checks flags in loaded chunks.
	 */
	private static void checkFlags(MinecraftServer server) {
		FactionData data = FactionData.get(server);
		for (Faction faction : List.copyOf(data.all())) {
			Iterator<FactionFlag> flags = List.copyOf(faction.flags()).iterator();
			while (flags.hasNext()) {
				FactionFlag flag = flags.next();
				ServerLevel level = server.getLevel(flag.dimension());
				if (level == null || !level.isLoaded(flag.pos())) {
					continue;
				}
				if (level.getBlockState(flag.pos()).getBlock() instanceof AbstractBannerBlock) {
					repaintFlag(level, flag, faction.color());
					FlagLabels.update(level, faction, flag);
				} else {
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

package com.crownofchaos.network;

import java.util.Optional;
import java.util.UUID;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the faction menu when the player does something. The server checks that the player
 * is allowed to, replies in chat, and sends everyone the updated factions.
 *
 * @param action what to do
 * @param text   a faction name (CREATE, RENAME) or player name (INVITE, DECLARE_WAR_ON_PLAYER);
 *               otherwise empty
 * @param target the player (CANCEL_INVITE, ACCEPT_REQUEST, DECLINE_REQUEST, KICK, TRANSFER,
 *               END_WAR_ON_PLAYER) or faction (ACCEPT_INVITE, DECLINE_INVITE, REQUEST_JOIN,
 *               CANCEL_REQUEST, SET_STANCE, DECLARE_WAR, END_WAR) acted on; otherwise empty
 * @param number a dye color id (CREATE, SET_COLOR), 1 or 0 (SET_RED_ALERT), or a
 *               {@link com.crownofchaos.faction.FactionStance} ordinal (SET_STANCE); otherwise 0
 */
public record FactionActionPayload(Action action, String text, Optional<UUID> target, int number) implements CustomPacketPayload {
	public enum Action {
		/** Found a faction named {@code text} with dye color {@code number}. */
		CREATE,
		/** Owner: rename the faction to {@code text}. */
		RENAME,
		/** Owner: change the faction's color to dye color {@code number}. */
		SET_COLOR,
		/** Owner: delete the faction. Its flags stop claiming land. */
		DISBAND,
		/** Member (not the owner): leave the faction. */
		LEAVE,
		/** Owner: invite the online player named {@code text}. */
		INVITE,
		/** Owner: take back the invite to player {@code target}. */
		CANCEL_INVITE,
		/** Accept faction {@code target}'s invite. */
		ACCEPT_INVITE,
		/** Turn down faction {@code target}'s invite. */
		DECLINE_INVITE,
		/** Ask to join faction {@code target}. */
		REQUEST_JOIN,
		/** Take back a request to join faction {@code target}. */
		CANCEL_REQUEST,
		/** Owner: let player {@code target}, who asked to join, in. */
		ACCEPT_REQUEST,
		/** Owner: turn down player {@code target}'s request to join. */
		DECLINE_REQUEST,
		/** Owner: remove member {@code target}. */
		KICK,
		/** Owner: make member {@code target} the owner. */
		TRANSFER,
		/** Owner: turn red alert on ({@code number} 1) or off (0). */
		SET_RED_ALERT,
		/** Owner: set the stance toward faction {@code target} to stance ordinal {@code number}. */
		SET_STANCE,
		/** Owner: declare war on faction {@code target}: your units attack its members and units, anywhere. */
		DECLARE_WAR,
		/** Owner: make peace with faction {@code target}. */
		END_WAR,
		/**
		 * Owner: declare war on the player named {@code text} alone (online, or a known member of
		 * any faction): your units attack them and their units, ignoring the rest of their faction.
		 */
		DECLARE_WAR_ON_PLAYER,
		/** Owner: make peace with player {@code target}. */
		END_WAR_ON_PLAYER,
		/** Anyone: send me all the factions again (asked when joining and when the menu opens, in case an earlier update was missed). */
		SYNC
	}

	public static final CustomPacketPayload.Type<FactionActionPayload> TYPE =
			new CustomPacketPayload.Type<>(CrownOfChaos.id("faction_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(i -> Action.values()[i], Action::ordinal), FactionActionPayload::action,
			ByteBufCodecs.stringUtf8(256), FactionActionPayload::text,
			ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), FactionActionPayload::target,
			ByteBufCodecs.VAR_INT, FactionActionPayload::number,
			FactionActionPayload::new);

	public static FactionActionPayload of(Action action) {
		return new FactionActionPayload(action, "", Optional.empty(), 0);
	}

	public static FactionActionPayload withText(Action action, String text) {
		return new FactionActionPayload(action, text, Optional.empty(), 0);
	}

	public static FactionActionPayload withTarget(Action action, UUID target) {
		return new FactionActionPayload(action, "", Optional.of(target), 0);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

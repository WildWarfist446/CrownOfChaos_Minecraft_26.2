package com.crownofchaos.lore;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.item.ModItems;
import com.crownofchaos.network.LoreBookPayload;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * "A Warning from a Former King": the lore book every player starts with, written as a warning from a
 * former crown-bearer. Its pages are in the language file ({@code lore.crownofchaos.page.N}); the
 * glitched words are marked {@code [[like this]]} and drawn scrambled. Each page past the first three
 * unlocks with an advancement (the one for the step before the secret it hints at), and the pages a
 * player has not unlocked are drawn scrambled in full.
 */
public final class LoreBook {
	/** The advancement that unlocks each page, in order (null: always readable). */
	private static final String[] UNLOCKED_BY = {
			null, null, null, "root", "all_crowns", "incomplete_crown_of_chaos", "enter_arena", "enter_arena",
			"defeat_chaos_king", "chaos_smithing_template", "crown_of_chaos"};

	public static final int PAGE_COUNT = UNLOCKED_BY.length;

	/** How often (in ticks) players are checked for having earned their book. */
	private static final int GIVE_CHECK_TICKS = 40;

	/** Set once a player has been given the book, so it is not given again. It survives death. */
	private static final AttachmentType<Boolean> GIVEN = AttachmentRegistry.create(CrownOfChaos.id("lore_book_given"),
			builder -> builder.persistent(Codec.BOOL).copyOnDeath());

	private LoreBook() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(LoreBookPayload.TYPE, LoreBookPayload.CODEC);
		// A player is given one, once, when they first have a Golden Crown (the book is the former king's warning,
		// and it finds you when the crown does). Checked every couple of seconds, so it also reaches players who
		// already had a Golden Crown before the book existed.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % GIVE_CHECK_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!Boolean.TRUE.equals(player.getAttached(GIVEN)) && isDone(player, "root")) {
					player.setAttached(GIVEN, true);
					ItemStack book = new ItemStack(ModItems.LORE_BOOK);
					if (!player.getInventory().add(book)) {
						player.drop(book, false);
					}
				}
			}
		});
	}

	/** Opens the book for this player, with the pages their advancements have unlocked. */
	public static void open(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, LoreBookPayload.TYPE)) {
			ServerPlayNetworking.send(player, new LoreBookPayload(unlockedPages(player)));
		}
	}

	private static int unlockedPages(ServerPlayer player) {
		int mask = 0;
		for (int page = 0; page < PAGE_COUNT; page++) {
			if (isDone(player, UNLOCKED_BY[page])) {
				mask |= 1 << page;
			}
		}
		return mask;
	}

	private static boolean isDone(ServerPlayer player, @Nullable String advancement) {
		if (advancement == null) {
			return true;
		}
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(CrownOfChaos.id(advancement));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}

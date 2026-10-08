package com.crownofchaos.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.crownofchaos.ability.CrownAbilities;
import com.crownofchaos.network.OreSensePayload;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client half of the Under Crown's Ore Sense: finds the ores around the player and outlines
 * each in its ore's color, through walls, using invisible glowing shulkers (a full-block outline)
 * that exist only on this client. They're removed when the time runs out or the player changes
 * world.
 *
 * <p>The markers used to be glowing block displays, but Create's renderer (with Flywheel) drops
 * the glowing outline of block displays, so in packs with Create no ores showed. Mob outlines,
 * like the shulker's, are drawn the vanilla way there too.
 */
public final class OreSenseClient {
	/** Most ores shown at once, nearest first, to keep the outlines readable. */
	private static final int MAX_MARKERS = 512;

	/** Outline color for each kind of ore. Ores from other mods in {@code c:ores} are white. */
	private static final Map<TagKey<Block>, Integer> ORE_COLORS = Map.of(
			ConventionalBlockTags.COAL_ORES, 0x9A9A9A,
			ConventionalBlockTags.IRON_ORES, 0xD8AF93,
			ConventionalBlockTags.COPPER_ORES, 0xE0734D,
			ConventionalBlockTags.GOLD_ORES, 0xFCEE4B,
			ConventionalBlockTags.REDSTONE_ORES, 0xFF2020,
			ConventionalBlockTags.LAPIS_ORES, 0x2A5BFF,
			ConventionalBlockTags.DIAMOND_ORES, 0x5DECF5,
			ConventionalBlockTags.EMERALD_ORES, 0x17DD62,
			ConventionalBlockTags.QUARTZ_ORES, 0xF0EDE6,
			ConventionalBlockTags.NETHERITE_SCRAP_ORES, 0x9A5B44);
	private static final int OTHER_ORE_COLOR = 0xFFFFFF;

	/** Markers currently shown. Only touched on the client thread. */
	private static final List<Entity> MARKERS = new ArrayList<>();
	private static ClientLevel markerLevel;
	private static long endsAtGameTime;
	/** Client-only entity ids, counting down from well below anything the server hands out. */
	private static int nextMarkerId = -1_000_000;

	private OreSenseClient() {
	}

	static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(OreSensePayload.TYPE, (payload, context) ->
				show(context.client(), payload.radius(), payload.durationTicks()));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (MARKERS.isEmpty()) {
				return;
			}
			if (client.level != markerLevel || client.level.getGameTime() >= endsAtGameTime) {
				clear();
				return;
			}
			// Drop the outline of any ore that has since been mined.
			Iterator<Entity> markers = MARKERS.iterator();
			while (markers.hasNext()) {
				Entity marker = markers.next();
				if (!markerLevel.getBlockState(marker.blockPosition()).is(ConventionalBlockTags.ORES)) {
					markerLevel.removeEntity(marker.getId(), Entity.RemovalReason.DISCARDED);
					markers.remove();
				}
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
	}

	private static void show(Minecraft client, int radius, int durationTicks) {
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			return;
		}
		clear();

		BlockPos center = client.player.blockPosition();
		List<BlockPos> ores = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (level.getBlockState(pos).is(ConventionalBlockTags.ORES)) {
				ores.add(pos.immutable());
			}
		}
		ores.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));

		for (BlockPos pos : ores.subList(0, Math.min(ores.size(), MAX_MARKERS))) {
			OreMarker marker = new OreMarker(level, colorOf(level.getBlockState(pos)));
			marker.setId(nextMarkerId--);
			marker.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
			level.addEntity(marker);
			MARKERS.add(marker);
		}
		markerLevel = level;
		endsAtGameTime = level.getGameTime() + durationTicks;

		Component message = ores.isEmpty()
				? Component.translatable("message.crownofchaos.ability.ore_sense.none")
				: Component.translatable("message.crownofchaos.ability.ore_sense.found", ores.size());
		client.player.sendOverlayMessage(message.copy().withStyle(CrownAbilities.MESSAGE_COLOR));
	}

	private static int colorOf(BlockState state) {
		for (Map.Entry<TagKey<Block>, Integer> entry : ORE_COLORS.entrySet()) {
			if (state.is(entry.getKey())) {
				return entry.getValue();
			}
		}
		return OTHER_ORE_COLOR;
	}

	private static void clear() {
		for (Entity marker : MARKERS) {
			if (marker.level() instanceof ClientLevel level) {
				level.removeEntity(marker.getId(), Entity.RemovalReason.DISCARDED);
			}
		}
		MARKERS.clear();
		markerLevel = null;
	}

	/**
	 * An invisible shulker that always glows in its ore's color, so only its full-block outline
	 * shows. The glowing flag and team color normally come from the server, which never hears of
	 * these client-only markers. It can't be hit or bumped into, so it never gets in the way of
	 * mining the ore it marks.
	 */
	private static final class OreMarker extends Shulker {
		private final int color;

		private OreMarker(Level level, int color) {
			super(EntityTypes.SHULKER, level);
			this.color = color;
			setInvisible(true);
			setNoAi(true);
			setSilent(true);
		}

		@Override
		public boolean isCurrentlyGlowing() {
			return true;
		}

		@Override
		public int getTeamColor() {
			return this.color;
		}

		@Override
		public boolean isPickable() {
			return false;
		}

		@Override
		public boolean canBeCollidedWith(Entity other) {
			return false;
		}

		@Override
		public boolean isPushable() {
			return false;
		}
	}
}

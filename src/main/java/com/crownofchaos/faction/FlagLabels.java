package com.crownofchaos.faction;

import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The floating name label above each faction flag: the faction's name in its color, with a
 * shadow on a faint dark background, always turned toward whoever is looking (like a name tag).
 * It's a vanilla text display, so every player sees it without anything extra installed.
 *
 * <p>Labels are tagged with their flag's position. They're kept in step every few seconds
 * (created if missing, updated after a rename or recolor), removed with their flag, and any
 * label whose flag is gone is removed when it loads.
 */
final class FlagLabels {
	private static final String TAG = "crownofchaos.flag_label";
	/** How high the label floats above the flag's block: just over the top of a standing banner. */
	private static final double STANDING_HEIGHT = 2.15;
	/** The same for a wall banner, which hangs from near the top of its block. */
	private static final double WALL_HEIGHT = 1.1;
	/** Faint dark background behind the text (ARGB), so it reads against sky and terrain. */
	private static final int BACKGROUND = 0x50000000;

	private FlagLabels() {
	}

	static void initialize() {
		// Remove labels left behind by flags that are gone (say, removed while their chunk was unloaded).
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Display.TextDisplay && entity.entityTags().contains(TAG)) {
				BlockPos pos = flagPosOf(entity.entityTags());
				if (pos == null || FactionData.get(level.getServer()).flagOwner(new FactionFlag(level.dimension(), pos)).isEmpty()) {
					entity.discard();
				}
			}
		});
	}

	/** Makes sure the flag has exactly one label, showing the faction's current name and color. */
	static void update(ServerLevel level, Faction faction, FactionFlag flag) {
		Component text = faction.displayName();
		List<Display.TextDisplay> labels = labelsOf(level, flag);
		if (labels.isEmpty()) {
			Display.TextDisplay label = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
			Vec3 at = labelPosition(level, flag.pos());
			label.setPos(at.x, at.y, at.z);
			label.setText(text);
			label.setBillboardConstraints(Display.BillboardConstraints.CENTER);
			label.setBackgroundColor(BACKGROUND);
			label.setFlags(Display.TextDisplay.FLAG_SHADOW);
			label.addTag(TAG);
			label.addTag(positionTag(flag.pos()));
			level.addFreshEntity(label);
			return;
		}
		Display.TextDisplay label = labels.getFirst();
		if (!text.equals(label.getText())) {
			label.setText(text);
		}
		labels.stream().skip(1).forEach(Display.TextDisplay::discard);
	}

	static void remove(ServerLevel level, FactionFlag flag) {
		labelsOf(level, flag).forEach(Display.TextDisplay::discard);
	}

	private static List<Display.TextDisplay> labelsOf(ServerLevel level, FactionFlag flag) {
		String tag = positionTag(flag.pos());
		AABB area = new AABB(flag.pos()).expandTowards(0.0, 3.0, 0.0).inflate(1.0);
		return level.getEntitiesOfClass(Display.TextDisplay.class, area, label -> label.entityTags().contains(tag));
	}

	private static Vec3 labelPosition(ServerLevel level, BlockPos pos) {
		boolean onWall = level.getBlockState(pos).getBlock() instanceof WallBannerBlock;
		return new Vec3(pos.getX() + 0.5, pos.getY() + (onWall ? WALL_HEIGHT : STANDING_HEIGHT), pos.getZ() + 0.5);
	}

	private static String positionTag(BlockPos pos) {
		return TAG + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	private static BlockPos flagPosOf(java.util.Set<String> tags) {
		for (String tag : tags) {
			if (tag.startsWith(TAG + "@")) {
				String[] parts = tag.substring(TAG.length() + 1).split(",");
				try {
					return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
				} catch (RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}
}

package com.crownofchaos.item;

import java.util.UUID;

import com.crownofchaos.CrownOfChaos;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

/** The mod's item data components. */
public final class ModComponents {
	/** The faction a Faction Flag belongs to, and its name when the flag was made (for the tooltip). */
	public record FlagFaction(UUID id, String name) {
		public static final Codec<FlagFaction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(FlagFaction::id),
				Codec.STRING.fieldOf("name").forGetter(FlagFaction::name)
		).apply(instance, FlagFaction::new));
		public static final StreamCodec<io.netty.buffer.ByteBuf, FlagFaction> STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, FlagFaction::id,
				ByteBufCodecs.STRING_UTF8, FlagFaction::name,
				FlagFaction::new);
	}

	public static final DataComponentType<FlagFaction> FLAG_FACTION = register("flag_faction",
			DataComponentType.<FlagFaction>builder().persistent(FlagFaction.CODEC).networkSynchronized(FlagFaction.STREAM_CODEC).build());

	/** A Faction Flag's color: its faction's. Separate so the item model can pick the banner color from it. */
	public static final DataComponentType<DyeColor> FLAG_COLOR = register("flag_color",
			DataComponentType.<DyeColor>builder().persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC).build());

	private ModComponents() {
	}

	private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, CrownOfChaos.id(name), type);
	}

	/** Loads this class so the components are registered. */
	public static void initialize() {
	}
}

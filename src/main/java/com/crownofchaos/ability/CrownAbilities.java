package com.crownofchaos.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.crownofchaos.item.ModItems;
import com.crownofchaos.network.CrownAbilityKeyPayload;
import com.crownofchaos.network.RiptidePayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

/**
 * Maps each crown to its ability and handles the ability key sent from the client.
 */
public final class CrownAbilities {
	private static final Map<Item, CrownAbility> ABILITIES = new HashMap<>();

	/** Players currently holding the ability key. Only touched on the server thread. */
	private static final Set<UUID> HOLDING_KEY = new HashSet<>();

	private CrownAbilities() {
	}

	/** Attaches an ability to a crown. Call during mod initialization. */
	public static void register(Item crown, CrownAbility ability) {
		if (ABILITIES.putIfAbsent(crown, ability) != null) {
			throw new IllegalStateException("Crown already has an ability: " + crown);
		}
	}

	/** Whether the player is holding down the "Use Crown Ability" key right now. */
	public static boolean isHoldingAbilityKey(ServerPlayer player) {
		return HOLDING_KEY.contains(player.getUUID());
	}

	public static void initialize() {
		register(ModItems.UNDER_CROWN, new VeinMinerAbility());
		register(ModItems.CROWN_OF_POSEIDON, new RiptideAbility());

		VeinMinerAbility.initialize();

		PayloadTypeRegistry.serverboundPlay().register(CrownAbilityKeyPayload.TYPE, CrownAbilityKeyPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RiptidePayload.TYPE, RiptidePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CrownAbilityKeyPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (payload.pressed()) {
				HOLDING_KEY.add(player.getUUID());
				tryActivate(player);
			} else {
				HOLDING_KEY.remove(player.getUUID());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HOLDING_KEY.remove(handler.getPlayer().getUUID()));
	}

	private static void tryActivate(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}

		ItemStack crown = player.getItemBySlot(EquipmentSlot.HEAD);
		CrownAbility ability = ABILITIES.get(crown.getItem());
		if (ability == null) {
			player.sendOverlayMessage(Component.translatable(crown.isEmpty()
					? "message.crownofchaos.ability.no_crown"
					: "message.crownofchaos.ability.no_power"));
			return;
		}

		ItemCooldowns cooldowns = player.getCooldowns();
		if (cooldowns.isOnCooldown(crown)) {
			int secondsLeft = Mth.ceil(cooldowns.getCooldownPercent(crown, 0.0F) * ability.cooldownTicks() / 20.0F);
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.cooldown", secondsLeft));
			return;
		}

		if (ability.activate(player, crown) && ability.cooldownTicks() > 0) {
			cooldowns.addCooldown(crown, ability.cooldownTicks());
		}
	}
}

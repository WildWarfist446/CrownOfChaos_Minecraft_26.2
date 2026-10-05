package com.crownofchaos.ability;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.crownofchaos.item.CrownEffects;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A crown ability giving 60 seconds of flight at a mob's flying speed, with that mob's particles
 * the whole time. It works like creative flight (double-tap jump; jump and sneak to rise and
 * sink). When it ends, the wearer gets Slow Falling until they land (up to 30 seconds), so they
 * can't take fall damage. Flight ends early if they take the crown off or die. Creative and
 * spectator players keep their own flight.
 *
 * <p>Blaze Flight (Crown of Nether) and Wither Flight (Crown of Wither) are instances of this.
 */
public final class FlightAbility implements CrownAbility {
	private static final int FLIGHT_TICKS = 1200;
	private static final float DEFAULT_FLYING_SPEED = 0.05F;
	/** Slow Falling after the flight lasts until landing, but no longer than this (30 seconds). */
	private static final int MAX_SAFE_LANDING_TICKS = 600;
	/** 2 minutes from takeoff. */
	private static final int COOLDOWN_TICKS = 2400;

	/**
	 * 40% of creative flight speed (0.05): slow, like a hovering Blaze.
	 */
	public static FlightAbility blazeFlight(Item crown) {
		return new FlightAbility("blaze_flight", crown, 0.02F, SoundEvents.BLAZE_AMBIENT, FlightAbility::emitBlazeEffects);
	}

	/**
	 * 40% of creative flight speed (0.05), the same as Blaze Flight.
	 */
	public static FlightAbility witherFlight(Item crown) {
		return new FlightAbility("wither_flight", crown, 0.02F, SoundEvents.WITHER_AMBIENT, FlightAbility::emitWitherEffects);
	}

	/** Players currently flying, and with which ability until when. Only touched on the server thread. */
	private static final Map<UUID, ActiveFlight> FLIGHTS = new HashMap<>();
	/** Server tick each player's post-flight Slow Falling runs out at the latest. */
	private static final Map<UUID, Integer> SAFE_LANDING_ENDS = new HashMap<>();
	/** Flying speeds of every flight ability; they also mark a flight in saved player data. */
	private static final Map<Float, FlightAbility> BY_SPEED = new HashMap<>();

	private final String name;
	private final Item crown;
	/** Also marks this flight in saved player data, so one cut short by a crash can be cleaned up. */
	private final float flyingSpeed;
	private final SoundEvent startSound;
	private final Consumer<ServerPlayer> effects;

	private FlightAbility(String name, Item crown, float flyingSpeed, SoundEvent startSound, Consumer<ServerPlayer> effects) {
		this.name = name;
		this.crown = crown;
		this.flyingSpeed = flyingSpeed;
		this.startSound = startSound;
		this.effects = effects;
		BY_SPEED.put(flyingSpeed, this);
	}

	@Override
	public boolean activate(ServerPlayer player, ItemStack crown) {
		if (player.isCreative() || player.isSpectator()) {
			player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability.flight.already_flying")
					.withStyle(CrownAbilities.MESSAGE_COLOR));
			return false;
		}

		Abilities abilities = player.getAbilities();
		abilities.mayfly = true;
		abilities.flying = true;
		abilities.setFlyingSpeed(this.flyingSpeed);
		player.onUpdateAbilities();

		int now = player.level().getServer().getTickCount();
		FLIGHTS.put(player.getUUID(), new ActiveFlight(this, now + FLIGHT_TICKS));
		SAFE_LANDING_ENDS.remove(player.getUUID());

		player.level().playSound(null, player, this.startSound, SoundSource.PLAYERS, 1.0F, 1.0F);
		showTimeLeft(player, FLIGHT_TICKS);
		return true;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(FlightAbility::tick);

		// End the flight before the player is saved, so leaving mid-flight doesn't keep it.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (FLIGHTS.remove(handler.getPlayer().getUUID()) != null) {
				removeFlight(handler.getPlayer());
			}
			SAFE_LANDING_ENDS.remove(handler.getPlayer().getUUID());
		});

		// Clean up a flight cut short by a crash or server stop.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!player.isCreative() && !player.isSpectator() && BY_SPEED.containsKey(player.getAbilities().getFlyingSpeed())) {
				removeFlight(player);
			}
		});
	}

	private static void tick(MinecraftServer server) {
		int now = server.getTickCount();

		Iterator<Map.Entry<UUID, ActiveFlight>> flights = FLIGHTS.entrySet().iterator();
		while (flights.hasNext()) {
			Map.Entry<UUID, ActiveFlight> entry = flights.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				flights.remove();
				continue;
			}

			FlightAbility ability = entry.getValue().ability();
			int ticksLeft = entry.getValue().endTick() - now;
			if (!player.isAlive() || !CrownEffects.isWearing(player, ability.crown) || ticksLeft <= 0) {
				flights.remove();
				ability.endFlight(player, now);
			} else {
				ability.effects.accept(player);
				if (ticksLeft == 200 || ticksLeft <= 100 && ticksLeft % 20 == 0) {
					// A warning at 10 seconds, then a countdown for the last 5.
					ability.showTimeLeft(player, ticksLeft);
				}
			}
		}

		Iterator<Map.Entry<UUID, Integer>> landings = SAFE_LANDING_ENDS.entrySet().iterator();
		while (landings.hasNext()) {
			Map.Entry<UUID, Integer> landing = landings.next();
			ServerPlayer player = server.getPlayerList().getPlayer(landing.getKey());
			if (player == null || !player.isAlive() || player.onGround() || player.isInWater() || now >= landing.getValue()) {
				landings.remove();
			} else if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, true));
			}
		}
	}

	private void endFlight(ServerPlayer player, int now) {
		removeFlight(player);
		if (player.isAlive() && !player.onGround()) {
			SAFE_LANDING_ENDS.put(player.getUUID(), now + MAX_SAFE_LANDING_TICKS);
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, true));
		}
		player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability." + this.name + ".ended")
				.withStyle(CrownAbilities.MESSAGE_COLOR));
	}

	/** Takes away flight, unless the player can fly anyway (creative or spectator). */
	private static void removeFlight(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		abilities.setFlyingSpeed(DEFAULT_FLYING_SPEED);
		if (!player.isCreative() && !player.isSpectator()) {
			abilities.mayfly = false;
			abilities.flying = false;
		}
		player.onUpdateAbilities();
	}

	private void showTimeLeft(ServerPlayer player, int ticksLeft) {
		player.sendOverlayMessage(Component.translatable("message.crownofchaos.ability." + this.name + ".time_left", (ticksLeft + 19) / 20)
				.withStyle(CrownAbilities.MESSAGE_COLOR));
	}

	/**
	 * The smoke and crackle a Blaze gives off (see vanilla {@code Blaze#aiStep}): two large smoke
	 * particles around the body every tick, and now and then a burning sound. Sent from the
	 * server so every nearby player sees and hears them.
	 */
	private static void emitBlazeEffects(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		RandomSource random = player.getRandom();
		for (int i = 0; i < 2; i++) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getRandomX(0.5), player.getRandomY(), player.getRandomZ(0.5),
					1, 0.0, 0.0, 0.0, 0.0);
		}
		if (random.nextInt(24) == 0) {
			level.playSound(null, player.getX() + 0.5, player.getY() + 0.5, player.getZ() + 0.5, SoundEvents.BLAZE_BURN,
					SoundSource.PLAYERS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
		}
	}

	/**
	 * The smoke a Wither trails from its heads (see vanilla {@code WitherBoss#aiStep}): one puff
	 * per head every tick, so three from the wearer's head.
	 */
	private static void emitWitherEffects(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		RandomSource random = player.getRandom();
		Vec3 head = player.getEyePosition();
		double radius = 0.3;
		for (int i = 0; i < 3; i++) {
			level.sendParticles(ParticleTypes.SMOKE, head.x + random.nextGaussian() * radius, head.y + random.nextGaussian() * radius,
					head.z + random.nextGaussian() * radius, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private record ActiveFlight(FlightAbility ability, int endTick) {
	}
}

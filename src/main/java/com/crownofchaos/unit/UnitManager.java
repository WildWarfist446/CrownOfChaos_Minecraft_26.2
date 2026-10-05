package com.crownofchaos.unit;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.faction.Faction;
import com.crownofchaos.faction.FactionData;
import com.crownofchaos.faction.FactionStance;
import com.crownofchaos.mixin.MobAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Army units in the world: the data that marks a mob as a unit, and the rules for whom units
 * fight.
 *
 * <p>Units are <b>passive</b>: they never pick fights, and don't fight back when hit, except
 * <ul>
 * <li>when told to attack: against players and units within {@link #ATTACK_RANGE} blocks of
 *     any faction it hasn't set to Allowed (including players with no faction),
 * <li>against factions, or single players, their faction is at war with, anywhere (see
 *     {@link #atWar}); war overrides the Allowed stance,
 * <li>inside their own faction's territory, against players and units of a faction it has set
 *     to Targeted, or, while it's on red alert, of any faction it hasn't set to Allowed
 *     (including players with no faction).
 * </ul>
 * Units never fight their own faction. The rule is applied through {@code Entity.isAlliedTo},
 * which every vanilla mob checks before targeting anything (see
 * {@link com.crownofchaos.mixin.EntityAlliesMixin}).
 */
public final class UnitManager {
	/** The unit data on a mob; saved with it and sent to every player (for the faction tint). */
	public static final AttachmentType<UnitData> UNIT = AttachmentRegistry.create(CrownOfChaos.id("unit"), builder -> builder
			.persistent(UnitData.CODEC)
			.syncWith(UnitData.STREAM_CODEC, AttachmentSyncPredicate.all()));

	/** Units told to attack go after enemies up to this many blocks away. */
	public static final double ATTACK_RANGE = 24.0;

	private UnitManager() {
	}

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			UnitData data = entity.getAttached(UNIT);
			if (data != null && entity instanceof Mob mob) {
				setUpUnit(mob);
				// Catch up on a faction color change made while the unit was unloaded.
				Faction faction = FactionData.get(level.getServer()).get(data.faction());
				if (faction != null && faction.color() != data.color()) {
					mob.setAttached(UNIT, data.withColor(faction.color()));
				}
			}
		});
	}

	/** Turns a newly created mob, before it's added to the world, into a unit of the player's faction. */
	public static void makeUnit(Mob mob, ServerPlayer owner, Faction faction, UnitType unit) {
		mob.setAttached(UNIT, new UnitData(owner.getUUID(), faction.id(), unit.id(), faction.color()));
		mob.setPersistenceRequired();
		// Piglins and Hoglins would otherwise turn into zombies outside the Nether.
		if (mob instanceof AbstractPiglin piglin) {
			piglin.setImmuneToZombification(true);
		}
		if (mob instanceof Hoglin hoglin) {
			hoglin.setImmuneToZombification(true);
		}
		// Its targeting is set up by the load event once it's added to the world.
	}

	/**
	 * Lets the unit go after anyone the rules say it should fight, players and other factions'
	 * units included, on top of its own vanilla targeting, and carry out its orders (see
	 * {@link UnitGoals}). Goals aren't saved, so this runs again whenever the unit loads. Mobs run by a "brain" instead of goals (Piglins, Hoglins,
	 * Wardens) only fight targets their own AI picks.
	 */
	private static void setUpUnit(Mob mob) {
		MobAccessor goals = (MobAccessor) mob;
		goals.crownofchaos$targetSelector().addGoal(1, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, true,
				(target, level) -> isHostile(mob, target)));
		goals.crownofchaos$goalSelector().addGoal(0, new UnitGoals.WatchGoal(mob));
		goals.crownofchaos$goalSelector().addGoal(1, new UnitGoals.OrdersGoal(mob));
		UnitGoals.keepWardenFromDigging(mob);
	}

	/** When a faction changes color, recolors its units in loaded chunks (the rest catch up when they load). */
	public static void recolor(MinecraftServer server, Faction faction) {
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				UnitData data = entity.getAttached(UNIT);
				if (data != null && data.faction().equals(faction.id()) && data.color() != faction.color()) {
					entity.setAttached(UNIT, data.withColor(faction.color()));
				}
			}
		}
	}

	/**
	 * Whether {@code unit} treats {@code other} as an ally: true unless the rules above say to
	 * fight it. Only called for units, on the server.
	 */
	public static boolean isAlly(Entity unit, Entity other) {
		return !(other instanceof LivingEntity living) || !isHostile(unit, living);
	}

	/** Whether the unit should fight the target (see the class description). */
	public static boolean isHostile(Entity unit, LivingEntity target) {
		UnitData data = unit.getAttached(UNIT);
		if (data == null || unit.level().getServer() == null || target == unit) {
			return false;
		}
		FactionData factions = FactionData.get(unit.level().getServer());
		Faction faction = factions.get(data.faction());
		if (faction == null) {
			// Its faction was disbanded: it no longer fights for anyone.
			return false;
		}
		UUID targetFaction = factionOf(target, factions);
		if (faction.id().equals(targetFaction)) {
			return false;
		}
		// A sentry doesn't chase anyone too far from its post.
		if (data.order() == UnitOrder.SENTRY && data.post().isPresent()
				&& target.position().distanceTo(Vec3.atBottomCenterOf(data.post().get())) > UnitGoals.SENTRY_LEASH) {
			return false;
		}
		if (atWar(faction, target, targetFaction)) {
			return true;
		}
		boolean playerOrUnit = target instanceof Player || target.hasAttached(UNIT);
		FactionStance stance = targetFaction == null ? FactionStance.NEUTRAL : faction.stanceToward(targetFaction);
		// Told to attack: any player or unit nearby that isn't allowed, wherever they are.
		if (data.order() == UnitOrder.ATTACK && playerOrUnit && stance != FactionStance.ALLOWED
				&& unit.distanceTo(target) <= ATTACK_RANGE) {
			return true;
		}
		// Otherwise only inside its own territory, and only players and units.
		Optional<Faction> territory = factions.territoryAt(target.level().dimension(), target.getX(), target.getZ());
		if (!playerOrUnit || territory.isEmpty() || territory.get() != faction) {
			return false;
		}
		return stance == FactionStance.TARGETED || faction.redAlert() && stance != FactionStance.ALLOWED;
	}

	/** The faction an entity fights for: a player's faction, or a unit's. */
	private static @Nullable UUID factionOf(LivingEntity entity, FactionData factions) {
		UnitData data = entity.getAttached(UNIT);
		if (data != null) {
			return data.faction();
		}
		if (entity instanceof Player player) {
			return factions.factionOf(player.getUUID()).map(Faction::id).orElse(null);
		}
		return null;
	}

	/**
	 * Whether the faction is at war with the target: with its whole faction, or with the player
	 * (or, for a unit, its owner) on their own. War overrides the Allowed stance.
	 */
	private static boolean atWar(Faction faction, LivingEntity target, @Nullable UUID targetFaction) {
		if (targetFaction != null && faction.warFactions().contains(targetFaction)) {
			return true;
		}
		UnitData unit = target.getAttached(UNIT);
		UUID player = unit != null ? unit.owner() : target instanceof Player ? target.getUUID() : null;
		return player != null && faction.warPlayers().containsKey(player);
	}
}

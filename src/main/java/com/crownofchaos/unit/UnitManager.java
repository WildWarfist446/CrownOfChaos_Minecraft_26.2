package com.crownofchaos.unit;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.faction.Faction;
import com.crownofchaos.faction.FactionData;
import com.crownofchaos.faction.FactionStance;
import com.crownofchaos.item.CrownEffects;
import com.crownofchaos.mixin.MobAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Army units in the world: the data that marks a mob as a unit, and the rules for whom units
 * fight.
 *
 * <p>Units are <b>passive</b>: they never pick fights with players or other units, and don't
 * fight back when hit, except
 * <ul>
 * <li>against wild hostile mobs (anything that is an {@code Enemy}, such as zombies, skeletons
 *     and creepers) that are attacking their owner or their faction's units (even while following,
 *     unless the follower is more than {@link #FOLLOW_FIGHT_RANGE} blocks from its leader), that come into a sentry's aggro range, or,
 *     when told to attack, are within {@link #ATTACK_RANGE} blocks,
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
	/** The unit id of units that are summoned by units (Evoker vexes): not in {@link ModUnits}. */
	public static final String MINION_ID = "minion";
	/** The most units one player can have summoned at once; reclaiming or losing units frees slots. */
	public static final int MAX_UNITS_PER_PLAYER = 40;
	/** A following unit stops fighting and rejoins its leader once it is this many blocks from them. */
	public static final double FOLLOW_FIGHT_RANGE = 40.0;
	/** A mob the owner hit this recently (10 seconds) is still the owner's target, so their units go for it. */
	private static final int OWNER_ATTACK_TICKS = 200;
	/** A mob that hurt the owner this recently (5 seconds) still counts as attacking them. */
	private static final int OWNER_HURT_TICKS = 100;

	private UnitManager() {
	}

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			adoptSummons(entity);
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

	/**
	 * How many units the player has summoned that are in loaded chunks (units in unloaded chunks
	 * count again once they load).
	 */
	public static int countUnits(MinecraftServer server, UUID owner) {
		int count = 0;
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				UnitData data = entity.getAttached(UNIT);
				if (data != null && data.owner().equals(owner) && entity.isAlive() && ModUnits.byId(data.unit()) != null) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Vexes called up by an Evoker unit join its faction as minions, so they fight for it instead
	 * of being wild vexes. Minions aren't a unit type: they can't be commanded or reclaimed and
	 * don't count toward the unit limit. They carry the owner and faction, so they get the faction
	 * color, the fighting rules and the unit goals. The Evoker sets the vex's owner before adding
	 * it, so this sees it on load.
	 */
	private static void adoptSummons(Entity entity) {
		if (entity instanceof Vex vex && !vex.hasAttached(UNIT) && vex.getOwner() != null) {
			UnitData summoner = vex.getOwner().getAttached(UNIT);
			if (summoner != null) {
				vex.setAttached(UNIT, new UnitData(summoner.owner(), summoner.faction(), MINION_ID, summoner.color()));
			}
		}
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
		// Drowned units always carry a trident to throw (a wild one rarely does). It never drops,
		// so summoning Drowned can't be used to farm tridents; thrown ones can't be picked up.
		if (mob instanceof Drowned) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
			mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
		// A follower drops any fight once it is way out of range of its leader, and rejoins them.
		if (data.order() == UnitOrder.FOLLOW && isFarFromLeader(unit, data)) {
			return false;
		}
		// Wild hostile mobs (not other units, even hostile-type ones): fought by a sentry (inside its leash, checked
		// above) when they come into its aggro range, when told to attack, or, whatever the order (following
		// included), when they're after the owner or their faction's units.
		if (target instanceof Enemy && !target.hasAttached(UNIT)
				&& (data.order() == UnitOrder.SENTRY || data.order() == UnitOrder.ATTACK && unit.distanceTo(target) <= ATTACK_RANGE
						|| isAggroOnUs(unit, data, target))) {
			return true;
		}
		boolean playerOrUnit = target instanceof Player || target.hasAttached(UNIT);
		FactionStance stance = targetFaction == null ? FactionStance.NEUTRAL : faction.stanceToward(targetFaction);
		// Whatever the owner is fighting, so units join in and then go back to their orders.
		if (stance != FactionStance.ALLOWED && isOwnerTarget(unit, data, target)) {
			return true;
		}
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

	/**
	 * Whether the mob is what the owner attacked within the last {@link #OWNER_ATTACK_TICKS}.
	 * Only mobs, not players (those follow the stances and war), and never the owner's own pets.
	 */
	private static boolean isOwnerTarget(Entity unit, UnitData data, LivingEntity target) {
		if (!(target instanceof Mob) || target.hasAttached(UNIT) && target.getAttached(UNIT).faction().equals(data.faction())) {
			return false;
		}
		Player owner = unit.level().getPlayerByUUID(data.owner());
		return owner != null && owner.isAlive() && owner.getLastHurtMob() == target
				&& owner.tickCount - owner.getLastHurtMobTimestamp() <= OWNER_ATTACK_TICKS
				&& !CrownEffects.isPetOf(target, owner);
	}

	/** Whether a following unit has strayed further than {@link #FOLLOW_FIGHT_RANGE} from the player it follows. */
	private static boolean isFarFromLeader(Entity unit, UnitData data) {
		Player leader = data.leader().map(id -> unit.level().getPlayerByUUID(id)).orElse(null);
		return leader != null && unit.distanceTo(leader) > FOLLOW_FIGHT_RANGE;
	}

	/**
	 * Whether the mob is aggro'd on us: going after the unit's owner or any unit of its faction,
	 * or it hurt the owner within the last {@link #OWNER_HURT_TICKS}.
	 */
	private static boolean isAggroOnUs(Entity unit, UnitData data, LivingEntity target) {
		if (target instanceof Mob mob && mob.getTarget() != null) {
			LivingEntity aimedAt = mob.getTarget();
			UnitData aimedUnit = aimedAt.getAttached(UNIT);
			if (aimedUnit != null && aimedUnit.faction().equals(data.faction()) || aimedAt.getUUID().equals(data.owner())) {
				return true;
			}
		}
		Player owner = unit.level().getPlayerByUUID(data.owner());
		return owner != null && owner.isAlive() && owner.getLastHurtByMob() == target
				&& owner.tickCount - owner.getLastHurtByMobTimestamp() <= OWNER_HURT_TICKS;
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

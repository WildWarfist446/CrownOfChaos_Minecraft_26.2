package com.crownofchaos.entity;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.crownofchaos.ability.FireChargeAbility;
import com.crownofchaos.ability.LightningStrikeAbility;
import com.crownofchaos.ability.WitherSkullAbility;
import com.crownofchaos.dimension.ChaosArena;
import com.crownofchaos.item.ModItems;
import com.crownofchaos.sound.ModSounds;
import com.crownofchaos.mixin.MobAccessor;
import com.crownofchaos.unit.UnitManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Herobrine, The Chaos King, the boss of the Chaos Arena (see {@link ChaosArena}): a player-style
 * fighter in Warfist's skin, wearing the Crown of Chaos.
 *
 * <p>The fight, from lighting the altar:
 * <ol>
 * <li><b>Rising:</b> he appears in the altar's fire in a storm of lightning bolts, speaks, and floats up
 *     about 5 blocks. He is invulnerable from the start.
 * <li><b>Waves:</b> he stays in the air and calls four waves of units, each starting once the last is
 *     dead, that attack players and players' units: 5 Strays and 5 Husks per player; then per player 1
 *     Ravager, 2 Evokers and 3 Vindicators; then per player 4 Guardians and 3 Drowned; then one Warden
 *     with 25% of its usual health for each player.
 * <li><b>Descending:</b> with the last wave dead he is no longer invulnerable and flies quickly down onto the altar.
 * <li><b>Fighting:</b> he attacks in melee and, every 30 seconds, uses one of three of the crowns'
 *     attacks (a Wither Skull, a Fire Charge or a Lightning Strike), never the same one twice in a row.
 *     He has a Warden's health. Beaten, he drops the Soul of Chaos and the altar's fire turns to soul
 *     fire (see {@link ChaosArena#onBossDefeated}).
 * </ol>
 * He never despawns, is not hurt by fire or lava, and does not drop the crown he wears.
 */
public final class ChaosKing extends Monster {
	/** Tags every unit the Chaos King calls up, so the fight knows which are his. */
	public static final String MINION_TAG = "crownofchaos.chaos_king_minion";
	/** Tags things his units make (the Evokers' vexes): on his side, but not counted for the end of a wave. */
	public static final String ALLY_TAG = "crownofchaos.chaos_king_ally";

	/** The same as a Warden's. */
	private static final double MAX_HEALTH = 500.0;
	private static final double MOVEMENT_SPEED = 0.3;
	private static final double ATTACK_DAMAGE = 8.0;
	private static final double FOLLOW_RANGE = 48.0;
	private static final double KNOCKBACK_RESISTANCE = 0.6;
	private static final double ARMOR = 8.0;

	/** How many blocks he floats up before the waves. */
	private static final double RISE_BLOCKS = 5.0;
	private static final double RISE_SPEED = 0.08;
	/** How fast he flies back down onto the altar, in blocks per tick (8 a second). */
	private static final double DESCENT_SPEED = 0.4;
	/** The bolts of lightning when he appears (at least 10), one every few ticks. */
	private static final int INTRO_BOLTS = 12;
	private static final int TICKS_BETWEEN_BOLTS = 3;
	/** The pause after a wave is cleared before the next one (8 seconds). */
	/** He is kept this many blocks in from the floor's edge, so he never walks into the lava wall. */
	private static final double EDGE_MARGIN = 2.0;
	/** His units appear on the floor between these distances from the altar, so they reach the players soon even in the big arena. */
	private static final double MINION_MIN_DISTANCE = 5.0;
	private static final double MINION_MAX_DISTANCE = 24.0;
	/** The laugh is 3 seconds long; he waits this long (in ticks) before he can start another. */
	private static final int LAUGH_COOLDOWN_TICKS = 80;
	/**
	 * The laugh reaches every player in the arena at the same loudness: each is sent the sound a couple of
	 * blocks from them, in the direction he is in, so it is not quieter for those far away.
	 */
	private static final double LAUGH_DISTANCE_FROM_LISTENER = 2.0;
	private static final float LAUGH_VOLUME = 1.5F;
	private static final int WAVE_PAUSE_TICKS = 120;
	/** The shorter pause after the last wave, before he comes down. */
	private static final int LAST_WAVE_PAUSE_TICKS = 80;
	/** How long after he finishes rising before the first wave (15 seconds): time to ready yourself and back away from the altar. */
	private static final int FIRST_WAVE_DELAY_TICKS = 300;
	/** The time between uses of his Wither Skull, Fire Charge and Lightning Strike (30 seconds), and the wait before the first. */
	private static final int ABILITY_COOLDOWN_TICKS = 240;
	private static final int FIRST_ABILITY_TICKS = 200;
	/** The Warden of the last wave has this share of its health for each player. */
	private static final double WARDEN_HEALTH_PER_PLAYER = 0.25;
	/** How many Souls of Chaos he drops. */
	private static final int SOULS_DROPPED = 1;

	private enum Stage {
		RISING, WAVES, DESCENDING, FIGHTING
	}

	private enum Ability {
		WITHER_SKULL, FIRE_CHARGE, LIGHTNING_STRIKE
	}

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

	private Stage stage = Stage.RISING;
	private int stageTicks;
	/** The wave called most recently, 0 before the first. */
	private int wave;
	/** How many players were in the arena when the altar was lit; every wave is sized by this, so a player dying (or arriving) mid-fight doesn't change the next one. 0 until then. */
	private int fightPlayers;
	/** Ticks since the last wave was cleared, while waiting to call the next. */
	private int pauseTicks = -1;
	private double hoverY = Double.NaN;
	private int abilityCooldown = FIRST_ABILITY_TICKS;
	/** Ticks until he may laugh again, so one moment (a lightning strike hitting three players) is one laugh. */
	private int laughCooldown;
	private @Nullable Ability lastAbility;

	public ChaosKing(EntityType<? extends ChaosKing> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.bossEvent.setDarkenScreen(true);
		this.xpReward = 0;
		// Never path through lava or fire, even though he is immune to them.
		this.setPathfindingMalus(PathType.LAVA, -1.0F);
		this.setPathfindingMalus(PathType.FIRE, -1.0F);
	}

	/**
	 * Whether the entity is the Chaos King or one of his legion (his units and what they make). The legion
	 * never fights itself: it counts as allied to itself (see {@code EntityAlliesMixin}) and its attacks
	 * do not hurt its own (see {@link ChaosArena#initialize}).
	 */
	public static boolean isLegion(Entity entity) {
		return entity instanceof ChaosKing || entity.entityTags().contains(MINION_TAG) || entity.entityTags().contains(ALLY_TAG);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
				.add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
				.add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
				.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
				.add(Attributes.ARMOR, ARMOR);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		// He fights players' army units too.
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, (target, level) -> target.hasAttached(UnitManager.UNIT)));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
			@Nullable SpawnGroupData groupData) {
		groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		// The Crown of Chaos on his head, which he never drops.
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CROWN_OF_CHAOS));
		this.setDropChance(EquipmentSlot.HEAD, 0.0F);
		return groupData;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	// The fight

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.stageTicks++;
		if (this.laughCooldown > 0) {
			this.laughCooldown--;
		}
		switch (this.stage) {
			case RISING -> this.tickRising(level);
			case WAVES -> this.tickWaves(level);
			case DESCENDING -> this.tickDescending(level);
			case FIGHTING -> this.tickFighting(level);
		}
		this.keepInArena();
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		// With every player dead or gone, the fight is over: it cleans itself up and has to be started again.
		if (this.stageTicks % 20 == 0 && ChaosArena.livingPlayers(level).isEmpty()) {
			ChaosArena.resetFight(level);
		}
	}

	/** He stays on the floor, away from the lava: pushed back from the edge, and put back on the altar if he ever ends up below the arena. */
	private void keepInArena() {
		double x = this.getX();
		double z = this.getZ();
		if (this.getY() < ChaosArena.FLOOR_Y - 1) {
			this.setDeltaMovement(Vec3.ZERO);
			this.teleportTo(ChaosArena.BOSS_LANDING.getX() + 0.5, ChaosArena.BOSS_LANDING.getY(), ChaosArena.BOSS_LANDING.getZ() + 0.5);
			return;
		}
		double distance = Math.sqrt(x * x + z * z);
		double max = ChaosArena.ARENA_RADIUS - EDGE_MARGIN;
		if (distance > max) {
			this.setPos(x * max / distance, this.getY(), z * max / distance);
			this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
			this.getNavigation().stop();
		}
	}

	/**
	 * An enchanted-looking diamond sword in his hand, for show: its attribute modifiers are emptied, so it adds
	 * no damage, and it has no real enchantments, only the glint. He never drops it.
	 */
	private void drawSword() {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		this.setItemSlot(EquipmentSlot.MAINHAND, sword);
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	/** His evil laugh, heard across the arena. Ignored while the last one is still playing. */
	public void laugh(ServerLevel level) {
		if (this.laughCooldown > 0) {
			return;
		}
		this.laughCooldown = LAUGH_COOLDOWN_TICKS;
		this.playToEveryone(level, ModSounds.CHAOS_KING_LAUGH);
	}

	/** Plays a sound of his to every player in the arena at the same loudness, from his direction (see {@link #LAUGH_DISTANCE_FROM_LISTENER}). */
	private void playToEveryone(ServerLevel level, SoundEvent soundEvent) {
		Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent);
		long seed = level.getRandom().nextLong();
		Vec3 king = this.position();
		for (ServerPlayer player : level.players()) {
			Vec3 toKing = king.subtract(player.position());
			Vec3 at = toKing.lengthSqr() < 1.0E-4 ? player.position()
					: player.position().add(toKing.normalize().scale(Math.min(LAUGH_DISTANCE_FROM_LISTENER, toKing.length())));
			player.connection.send(new ClientboundSoundPacket(sound, SoundSource.HOSTILE, at.x, at.y, at.z, LAUGH_VOLUME, 1.0F, seed));
		}
	}

	private void setStage(Stage stage) {
		this.stage = stage;
		this.stageTicks = 0;
	}

	/** He appears in lightning, says something and floats up. Invulnerable from here until the last wave is dead. */
	private void tickRising(ServerLevel level) {
		if (this.stageTicks == 1) {
			this.fightPlayers = Math.max(1, this.arenaPlayers(level).size());
			this.setNoAi(true);
			this.setNoGravity(true);
			this.setInvulnerable(true);
			this.hoverY = this.getY() + RISE_BLOCKS;
			level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
			this.say(level, "YOU DARE?", "Light my altar, and watch my legion crush you.");
		}
		// A storm of lightning around the altar: visual only, so it looks and sounds right without hurting anyone or lighting fires.
		if (this.stageTicks % TICKS_BETWEEN_BOLTS == 0 && this.stageTicks / TICKS_BETWEEN_BOLTS <= INTRO_BOLTS) {
			RandomSource random = level.getRandom();
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(this.getX() + (random.nextDouble() - 0.5) * 10.0, ChaosArena.FLOOR_Y + 1, this.getZ() + (random.nextDouble() - 0.5) * 10.0);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}
		if (this.getY() < this.hoverY) {
			this.setPos(this.getX(), Math.min(this.getY() + RISE_SPEED, this.hoverY), this.getZ());
		} else if (this.stageTicks > INTRO_BOLTS * TICKS_BETWEEN_BOLTS) {
			this.setStage(Stage.WAVES);
		}
		this.faceNearestPlayer(level);
	}

	/** He hovers while his legion fights; each wave is called when the one before it is dead. */
	private void tickWaves(ServerLevel level) {
		this.setPos(this.getX(), this.hoverY, this.getZ());
		this.faceNearestPlayer(level);
		if (this.stageTicks % 20 == 0) {
			this.keepWardensUp(level);
			this.catchUpLatecomers(level);
		}

		if (this.wave > 0 && this.minions(level).isEmpty()) {
			if (this.pauseTicks < 0) {
				this.pauseTicks = 0;
				if (this.wave >= 4) {
					this.say(level, "MY LEGION FALLS", "Then I will finish you myself.");
				} else {
					this.say(level, "WAVE " + this.wave + " CLEARED", "Wave " + (this.wave + 1) + " arrives in " + WAVE_PAUSE_TICKS / 20 + " seconds.");
				}
			}
			this.pauseTicks++;
		}
		if (this.wave == 0 && this.stageTicks == 1) {
			this.say(level, "RUN WHILE YOU CAN", "My legion arrives in " + FIRST_WAVE_DELAY_TICKS / 20 + " seconds.");
		}
		boolean ready = this.wave == 0 ? this.stageTicks >= FIRST_WAVE_DELAY_TICKS
				: this.pauseTicks >= (this.wave >= 4 ? LAST_WAVE_PAUSE_TICKS : WAVE_PAUSE_TICKS);
		if (ready) {
			this.pauseTicks = -1;
			if (this.wave >= 4) {
				// The last wave is dead: he draws his sword and comes down.
				this.drawSword();
				this.setInvulnerable(false);
				this.setStage(Stage.DESCENDING);
				return;
			}
			this.wave++;
			this.callWave(level, this.wave);
		}
	}

	/** He flies down onto the altar's top, in the middle of the arena, then fights. */
	private void tickDescending(ServerLevel level) {
		double landingX = ChaosArena.BOSS_LANDING.getX() + 0.5;
		double landingZ = ChaosArena.BOSS_LANDING.getZ() + 0.5;
		double landingY = ChaosArena.BOSS_LANDING.getY();
		double x = this.getX() + Mth.clamp(landingX - this.getX(), -DESCENT_SPEED, DESCENT_SPEED);
		double z = this.getZ() + Mth.clamp(landingZ - this.getZ(), -DESCENT_SPEED, DESCENT_SPEED);
		double y = Math.max(landingY, this.getY() - DESCENT_SPEED);
		this.setPos(x, y, z);
		this.faceNearestPlayer(level);
		if (y <= landingY && Math.abs(x - landingX) < 0.01 && Math.abs(z - landingZ) < 0.01) {
			this.setNoGravity(false);
			this.setNoAi(false);
			this.setStage(Stage.FIGHTING);
		}
	}

	/** Melee, and one of the crowns' attacks every 30 seconds. */
	private void tickFighting(ServerLevel level) {
		if (this.abilityCooldown > 0) {
			this.abilityCooldown--;
			return;
		}
		LivingEntity target = this.getTarget();
		if (target != null && target.isAlive() && this.hasLineOfSight(target)) {
			this.useAbility(level, target);
			this.abilityCooldown = ABILITY_COOLDOWN_TICKS;
		}
	}

	/** Uses a Wither Skull, a Fire Charge or a Lightning Strike on the target, not the same one twice running. */
	private void useAbility(ServerLevel level, LivingEntity target) {
		Ability[] abilities = Ability.values();
		Ability chosen;
		do {
			chosen = abilities[this.random.nextInt(abilities.length)];
		} while (chosen == this.lastAbility);
		this.lastAbility = chosen;

		this.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
		Vec3 direction = target.getEyePosition().subtract(this.getEyePosition());
		switch (chosen) {
			case WITHER_SKULL -> WitherSkullAbility.shoot(this, direction);
			case FIRE_CHARGE -> FireChargeAbility.shoot(this, direction);
			case LIGHTNING_STRIKE -> LightningStrikeAbility.strike(this, target);
		}
	}

	private void faceNearestPlayer(ServerLevel level) {
		Player player = level.getNearestPlayer(this, FOLLOW_RANGE);
		if (player != null) {
			this.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
		}
	}

	/** His words, as a banner across every player's screen: the line as the title, then a smaller line under it. */
	private void say(ServerLevel level, String title, String subtitle) {
		for (ServerPlayer player : level.players()) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
			player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.RED)));
			player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
		}
	}

	// The waves

	private AABB arenaBox() {
		return new AABB(-ChaosArena.ARENA_RADIUS, ChaosArena.FLOOR_Y - 2, -ChaosArena.ARENA_RADIUS,
				ChaosArena.ARENA_RADIUS, ChaosArena.FLOOR_Y + 40, ChaosArena.ARENA_RADIUS);
	}

	/** The living players in the arena: how big the waves are. */
	private List<ServerPlayer> arenaPlayers(ServerLevel level) {
		return ChaosArena.livingPlayers(level);
	}

	/** The units of his that are still alive. */
	private List<Mob> minions(ServerLevel level) {
		return level.getEntitiesOfClass(Mob.class, this.arenaBox(), mob -> mob.isAlive() && mob.entityTags().contains(MINION_TAG));
	}

	private void callWave(ServerLevel level, int number) {
		List<ServerPlayer> players = this.arenaPlayers(level);
		if (this.fightPlayers <= 0) {
			this.fightPlayers = Math.max(1, players.size());
		}
		int count = this.fightPlayers;
		String title = switch (number) {
			case 1 -> "THE DEAD RISE";
			case 2 -> "TEAR THEM APART";
			case 3 -> "DROWN THEM";
			default -> "WAKE, WARDEN";
		};
		this.say(level, title, "Wave " + number + " of 4");
		this.laugh(level);
		level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 0.6F);

		if (number < 4) {
			this.spawnWaveUnits(level, number, count);
		} else {
			for (Mob mob : this.spawnMinions(level, EntityTypes.WARDEN, 1, 0)) {
				// A quarter of a Warden's health for each player in the fight.
				double health = mob.getMaxHealth() * WARDEN_HEALTH_PER_PLAYER * count;
				mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
				mob.setHealth((float) health);
				if (mob instanceof Warden warden) {
					for (ServerPlayer player : players) {
						warden.increaseAngerAt(player, 150, false);
					}
				}
			}
		}
	}

	/** The units of waves 1 to 3 for {@code count} players' worth. */
	private void spawnWaveUnits(ServerLevel level, int number, int count) {
		switch (number) {
			case 1 -> {
				this.spawnMinions(level, EntityTypes.STRAY, 5 * count, 0);
				this.spawnMinions(level, EntityTypes.HUSK, 5 * count, 0);
			}
			case 2 -> {
				this.spawnMinions(level, EntityTypes.RAVAGER, count, 0);
				this.spawnMinions(level, EntityTypes.EVOKER, 2 * count, 0);
				this.spawnMinions(level, EntityTypes.VINDICATOR, 3 * count, 0);
			}
			default -> {
				this.spawnMinions(level, EntityTypes.GUARDIAN, 4 * count, 2);
				this.spawnMinions(level, EntityTypes.DROWNED, 3 * count, 0);
			}
		}
	}

	/**
	 * Players who arrive after the altar was lit: the fight counts them from now on, and the wave in
	 * progress gets their share of units (or, for the Warden, their share of its health) added.
	 */
	private void catchUpLatecomers(ServerLevel level) {
		List<ServerPlayer> players = this.arenaPlayers(level);
		int extra = players.size() - this.fightPlayers;
		if (this.fightPlayers <= 0 || extra <= 0) {
			return;
		}
		int before = this.fightPlayers;
		this.fightPlayers = players.size();
		List<Mob> alive = this.minions(level);
		if (this.wave < 1 || alive.isEmpty()) {
			return;
		}
		this.say(level, "MORE FOOLS ARRIVE", "My legion grows to meet you.");
		if (this.wave < 4) {
			this.spawnWaveUnits(level, this.wave, extra);
			return;
		}
		for (Mob mob : alive) {
			if (mob instanceof Warden warden) {
				double share = mob.getMaxHealth() / before * extra;
				warden.getAttribute(Attributes.MAX_HEALTH).setBaseValue(mob.getMaxHealth() + share);
				warden.heal((float) share);
				for (ServerPlayer player : players) {
					warden.increaseAngerAt(player, 150, false);
				}
			}
		}
	}

	/** Spawns units at random spots on the floor (lifted by {@code lift} blocks) and sets them up as his. */
	private <T extends Mob> List<T> spawnMinions(ServerLevel level, EntityType<T> type, int count, int lift) {
		java.util.ArrayList<T> spawned = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double distance = MINION_MIN_DISTANCE + this.random.nextDouble() * (MINION_MAX_DISTANCE - MINION_MIN_DISTANCE);
			BlockPos pos = BlockPos.containing(Math.cos(angle) * distance, ChaosArena.FLOOR_Y + 1 + lift, Math.sin(angle) * distance);
			T mob = type.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
			if (mob != null) {
				setUpMinion(mob);
				spawned.add(mob);
			}
		}
		return spawned;
	}

	/** Marks a mob as one of the Chaos King's units, so it fights players and their army units. */
	private static void setUpMinion(Mob mob) {
		mob.addTag(MINION_TAG);
		mob.setPersistenceRequired();
		MobAccessor accessor = (MobAccessor) mob;
		accessor.crownofchaos$targetSelector().addGoal(2, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, true,
				(target, level) -> target.hasAttached(UnitManager.UNIT)));
		if (mob instanceof Guardian) {
			// Guardians swim through the air here (see UnitWaterMixin), so they need a navigation that works in it.
			FlyingPathNavigation navigation = new FlyingPathNavigation(mob, mob.level());
			navigation.setCanFloat(true);
			accessor.crownofchaos$setNavigation(navigation);
		}
	}

	/** Keeps his Wardens from digging into the floor, which would remove them. */
	private void keepWardensUp(ServerLevel level) {
		for (Mob mob : this.minions(level)) {
			if (mob instanceof Warden && mob.getBrain().checkMemory(MemoryModuleType.DIG_COOLDOWN, MemoryStatus.REGISTERED)) {
				mob.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, net.minecraft.util.Unit.INSTANCE, 1200L);
			}
		}
	}

	// Death

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.spawnAtLocation(level, new ItemStack(ModItems.SOUL_OF_CHAOS, SOULS_DROPPED));
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level) {
			// His last cry, heard by everyone, before the altar's soul fire rises.
			this.playToEveryone(level, ModSounds.CHAOS_KING_DEATH);
			ChaosArena.onBossDefeated(level);
		}
	}

	// Saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Stage", this.stage.ordinal());
		output.putInt("StageTicks", this.stageTicks);
		output.putInt("Wave", this.wave);
		output.putInt("FightPlayers", this.fightPlayers);
		output.putDouble("HoverY", Double.isNaN(this.hoverY) ? this.getY() : this.hoverY);
		output.putInt("AbilityCooldown", this.abilityCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.stage = Stage.values()[Mth.clamp(input.getIntOr("Stage", 0), 0, Stage.values().length - 1)];
		this.stageTicks = input.getIntOr("StageTicks", 0);
		this.wave = input.getIntOr("Wave", 0);
		this.fightPlayers = input.getIntOr("FightPlayers", 0);
		this.hoverY = input.getDoubleOr("HoverY", this.getY());
		this.abilityCooldown = input.getIntOr("AbilityCooldown", FIRST_ABILITY_TICKS);
		if (this.hasCustomName()) {
			this.bossEvent.setName(this.getDisplayName());
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}
}

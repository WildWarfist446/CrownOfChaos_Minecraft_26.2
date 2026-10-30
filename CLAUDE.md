# CLAUDE.md

Context for Claude sessions working on this repository. Claude Code reads this file
automatically when a session starts in this folder; it can also be pasted into any Claude
chat. Read it, then [README.md](README.md) (what the mod does) and
[DEVELOPMENT.md](DEVELOPMENT.md) (how the code is organized) before changing anything.

## The project

**The Crown of Chaos** is a Fabric mod for **Minecraft 26.2** by **Warfist** (GitHub:
WildWarfist446). Players craft a Golden Crown, then upgrade it into magical crowns worn in
the helmet slot. Each crown has passive effects, a primary ability (R key) and often a
secondary ability (G key), plus a netherite upgrade.

- Repo: https://github.com/WildWarfist446/CrownOfChaos_Minecraft_26.2 (branch `main`).
- Mod ID `crownofchaos`, package `com.crownofchaos`.
- **TedKruznir** is a collaborator who uploads Blockbench 3D models to `3dmodels/` through
  the GitHub website. Warfist also edits files (README, `developing plans/Crown Ideas.md`)
  directly on GitHub.
- All art is hand-drawn by the mod's creators; the code is written with AI. The README says
  so. Never generate or replace art; use the provided sprites and models.

## Environment

- Java **25** is required (Minecraft 26.2). Gradle 9 with Fabric Loom; Minecraft 26.x is
  **unobfuscated**, so the code uses Mojang's names directly. No mappings setup.
- The original machine is Windows: JDK at
  `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`, project at
  `C:\Users\19dev\OneDrive\Desktop\CrownOfChaosMod`. On another machine, find the local
  JDK 25 and repo path instead of assuming these.
- A second Windows machine has the repo at `C:\GIT REPO\CrownOfChaos_Minecraft_26.2` (not in
  OneDrive). Its default Java is 27, which Gradle can't run on, so Gradle is pointed at the
  Temurin JDK 25 by `org.gradle.java.home` in `~/.gradle/gradle.properties`, and VS Code by
  `jdk.jdkhome` in its user settings. Python is installed there per user, and may need
  calling by its full path (`~/AppData/Local/Programs/Python/Python314/python.exe`) until
  the shell restarts.
- The project lives in OneDrive, which Gradle warns about; it works, but file locks are
  possible.
- Shell quirk: on Windows, bash heredocs break on apostrophes in text (for example, item
  descriptions). For multi-file edits, write a small Python script to the scratchpad and run
  it, or use the file-editing tools.

## Building and testing

```
./gradlew build                      # compile and package (build/libs/)
./gradlew runClient                  # launch the dev client
./gradlew genSources                 # decompile Minecraft for reading its source
```

After every change: run `./gradlew build`, then start the client and scan its log for
errors. Mixin mistakes only show up at runtime, and some only when a world loads (the
`EnderDragonFight` mixin once crashed world loading). To test inside a world, launch with
`--args="--quickPlaySingleplayer=New World"`.

**Testing without pressing keys:** a session can't press keys in the game. To check
something visually, add a temporary debug class registered from `CrownOfChaosClient`. It can
run steps on client ticks, run server steps through `client.getSingleplayerServer().execute`,
open screens, and save screenshots with `net.minecraft.client.Screenshot.grab(client, false)`
to `run/screenshots/`, which you then view. Set `pauseOnLostFocus:false` in `run/options.txt`
while testing, because the game window isn't focused. Afterward, delete the debug class and the
screenshots, and set the option back to `true`.

**Warfist tests from VS Code too,** using the same `build/` and `run/` folders. Don't run
`./gradlew build` or `runClient` while his game is open: a rebuild briefly deletes the
compiled classes and crashes his launch. Only stop game processes you started. VS Code's run
settings (`.vscode/`, not in Git) come from `./gradlew vscode`.

**The dev client's player name is fixed** to `Warfist` (`--username` in `build.gradle`).
Without it, Minecraft picks a random `PlayerNNN` on every launch; offline player IDs come from
the name, so each relog was a different player and lost its faction membership and unit
ownership (units stopped responding to commands). Worlds made before this fix still hold
factions and units owned by old random IDs. For VS Code, run `./gradlew vscode` again so its
run settings pick up the name.

Ignore these harmless log lines: Windows `PerfCounterQuery`/`oshi` stack traces (including
`CrashReport.preload`), "Could not authorize you against Realms", and "Failed to retrieve
profile key pair" (the dev client plays offline).

**Copy vanilla behavior from the source, never from memory.** 26.2 differs from older
versions in many places: `Identifier` (not `ResourceLocation`), the world clock system for
time of day, `SubmitNodeCollector` rendering, `KeyMapping.Category`, `BlocksAttacks` shield
logic, data-driven enchantments, and more. After `genSources`, the decompiled jars are in
`.gradle/loom-cache/minecraftMaven/net/minecraft/` (`minecraft-common-*-sources.jar` and
`minecraft-clientOnly-*-sources.jar`); unzip them somewhere temporary and grep. Fabric API
classes can be inspected with `javap` from the jars in `~/.gradle/caches/modules-2/`.

## How Warfist likes to work

- **"Commit the changes and update the readme"** is a standing request: check the README
  (and DEVELOPMENT.md) for anything out of date, commit with a descriptive message, and
  push. Before committing, always `git fetch` and look at new remote commits: Warfist and
  Ted edit on GitHub, so merge their work in and never overwrite it. If a local file and
  the remote both changed, compare them and ask or explain before discarding anything.
- Commit or push only when asked.
- Warfist isn't a confident speller: use the correct spelling for names that appear in game
  or in IDs (for example, "Poseidon", not "Posiden") and mention the fix. Keep deliberate
  jokes and lore text exactly as written.
- When a request leaves something out (cooldowns, levels, durations), pick a sensible
  default, say what you picked, and make it a named constant that's easy to change.
- Sprites live in `Sprites/`, but Warfist sometimes saves to
  `OneDrive\Pictures\MinecraftModdingSprites` instead. When asked to use a sprite, check
  both and use the newest; say which.
- Before saying a feature works, say whether it was actually tested in game. Most changes
  are verified by building and launching, not by playing.

## Standing rules for the mod

- **New crowns:** create them with `ModItems.registerCrown(...)` (adds the enchanted glint (not on the Golden Crown),
  description line and helmet stats) and give every crown except the Golden Crown a
  netherite version with `registerNetheriteCrown(...)`, a smithing recipe and an unlock
  advancement. Follow the "Adding a crown" checklist in DEVELOPMENT.md, and add the crown to
  the README: its section, the Abilities table, "Finding the items" and the 3D models list.
- **Crown checks:** use `CrownEffects.isWearing(...)` or `ModItems.baseCrown(...)`, never
  compare the head item directly, so netherite crowns get the same powers.
- **Recipes:** every crown recipe has the Golden Crown in the middle slot and a recipe-book
  unlock advancement keyed to its rarest ingredient.
- **Messages:** ability messages use `CrownAbilities.MESSAGE_COLOR` (light purple). Toggle
  abilities use `ToggleState`, which shows green "enabled" and red "disabled" messages.
- **Pets:** crown abilities never hurt the wearer's own tamed animals. Tag every entity an
  ability creates with `CrownAbilities.CROWN_ABILITY_TAG`, and skip `CrownEffects.isPetOf`
  targets in on-hit effects. Other players' pets can still be hit.
- **No griefing:** crown explosions don't break blocks, and crown lightning doesn't start
  fires or transform mobs.
- **Shared helpers:** reuse `ToggleState`, `ChargeTracker`, `AbilityTargeting`,
  `FlightAbility` and the terrain-Swiftness map in `CrownEffects` instead of writing new
  versions (see DEVELOPMENT.md).
- **Player data:** anything that changes saved player state (like flight) must be undone
  on disconnect and cleaned up on join, in case of a crash.
- **3D models:** every crown shows its 3D model both worn and as the item (not the sprite).
  Blockbench "Java Item" exports become `models/item/<crown>_worn.json` (see "Crown 3D
  models" below). Older "Modded Entity" exports (Mojang mappings) become classes in
  `client/model/`, nesting the crown under a `head` part (player-head template exports sit 24
  pixels too low and include template head cubes to remove) and are added to
  `CrownSpecialRenderer` for the item. Register each model for both the crown and its
  netherite version, and check it in game from the front.

## Current state (6 October 2026)

Done: the Golden Crown plus nine magical crowns (Under, Poseidon, Nether, Darkness, Sun,
Wither, Winter, Iron, Void), each with a netherite version, description, recipe and
abilities (the Under Crown's secondary is Ore Sense, the Crown of Winter's is Icicle
Drop); materials (Sculk Cord from Wardens, Ankh Charm in desert pyramids); a Dragon Egg
on every dragon kill; the Crown of Chaos creative tab; the faction system (beta, see below).
**Crown of Chaos, started:** the Incomplete Crown of Chaos material (`ModItems.INCOMPLETE_CROWN_OF_CHAOS`):
a shapeless recipe of the nine magical crowns, each slot taking the crown or its netherite
version; epic, stacks to 1, fire-resistant; unlocked by picking up a Crown of Void. Its sprite
is Warfist's 2048x2048 glitched collage (`Sprites/Incomplete CrownOfChaos.png`), scaled to
128x128 for `textures/item/incomplete_crown_of_chaos.png` (a 2048 item sprite would make a
huge in-hand model). Its description (Warfist's) is "ERROR ERROR ERR" plus six letters
(`ModItems.INCOMPLETE_CROWN_GLYPHS`, "awaken", Claude's pick) in the Standard Galactic Alphabet
(vanilla's `minecraft:alt` font), scrambled like `§k` (obfuscated) so they keep changing, built in code because a language file can't set a font. Next per `developing plans/To Do.md`: the boss, the smithing step and the
finished crown. **Crown of Chaos item** (`ModItems.CROWN_OF_CHAOS`, 19 October 2026): netherite helmet stats, fire-resistant, no netherite version, no description or recipe yet; creative tab only. **Its powers** (19 October 2026): the Crown of Nether's fire/lava immunity (Fire Resistance in `CrownEffects.EFFECTS`), the Wither immunity (`LivingEntityEffectMixin` and the tick that clears Wither), freeze immunity (the `freeze_immune_wearables` tag) and the Crown of Void's teleport dodge, through `CrownEffects.isWearingOrChaos` (only those four, not the on-hit powers; Water Breathing isn't an immunity in the plan, so it's not included); primary "The pain of death" (`PainOfDeathAbility`, 10 s, shortened from 30 at Warfist's request: the Wither skull and fireball always, plus the lightning on a target within 30 blocks and the Arrow Volley within 20, using the others' `shoot`/`strike`/`begin` helpers and none of their charges or cooldowns); secondary "Death's door" (`DarknessPulseAbility(4.0)`, `CrownAbilities.DEATHS_DOOR_DURATION_MULTIPLIER`: "300% longer" read as 4 times as long, the 3 times reading being the alternative); `UnitCommands.wearsCrownFor` lets it command every unit type, and `ModUnits.forCrown` returns all of them for it, so a Commander's Table with the Crown of Chaos in the crown slot lists and makes every unit (tested: 18 units listed, a Guardian unit made from its costs; the Wither crown still lists 1). Tested in game on a survival player: Wither refused, no freezing, Fire Resistance on, a hit teleports 5 blocks, the pain of death spawns a skull, a fireball, a lightning bolt (the cow lost 4) and 14 arrows, and it commands a Guardian unit while the Wither crown doesn't. Not tested: Death's door (it needs a second player), a real fight. **Fixed 19 October 2026 (Warfist: "the powers don't seem to be all firing"):** the skull and fireball spawn together and hit each other, the skull vanishing mid-air at 4 blocks; `mixin/ProjectileMixin` makes a player's own crown-ability projectiles pass through each other (same owner, both with `CROWN_ABILITY_TAG`). Tested: skull, fireball, lightning and 24 arrows all reach the target now. Its 3D model is a Java Item model like Sun's (`3dmodels/CrownOfChaos.json` and `.bbmodel`, 128x128 own texture, `models/item/crown_of_chaos_worn.json`, head transform `[0, 5.28, 0]` scale 1.44 (Warfist said the default 1.6 was a bit big; same as the Golden Crown), the reference head cube dropped); seen in game worn (front and back) and held. Re-saved by Warfist on 23 October 2026 (12 more pieces, a new texture); the worn model was rebuilt from the new export with the head transform kept as it was. **The Chaos Arena** (boss dimension, 23 October 2026, in progress; Warfist's plan: a dimension reached by throwing Disc 11 into a nether portal, whose frame turns to "null" blocks and random unchanging blocks, and breaking the null blocks destroys the portal; none of the portal exists yet). Built so far: `crownofchaos:chaos_arena`, defined in data (`dimension/`, `dimension_type/` copied from the Nether's with a dark purple ambient light and fog, `worldgen/biome/chaos_void.json` with fog `#0d0219`, a flat generator of nothing) and `dimension/ChaosArena`: on server start (and `enter`) it builds a floating disc of radius `ARENA_RADIUS` 18 at y 64 (a glitched random mix of netherrack, cobblestone, mossy cobblestone and nether bricks, the same mix each time for a given spot, with bedrock under it) and the Herobrine altar at the centre (3x3 gold blocks standing on the floor, so their sides show (Warfist had it one block too low at first), a netherrack on top of the middle, redstone torches on the four gold blocks beside it). Players arrive 14 blocks south of the altar via `ChaosArena.enter(player)` (the future portal will call it; until then, `/execute in crownofchaos:chaos_arena run tp @s 0 65 -14`). **Arena size** (Warfist, 23 October 2026: asked for "triple the size", tried radius 54, then "a little too big, back to double"): `ARENA_RADIUS` 36, about 73 blocks across (the first version was 18); `ARRIVAL` (z -32) and `EXIT_PORTAL` (z -34, with a bedrock row behind it, which stops walking through it leading to the lava) are derived from it; his units spawn 5 to 24 blocks from the altar (`MINION_MIN/MAX_DISTANCE`); an arena of an earlier size (`LEGACY_ARENA_RADII` 18 and 54) in an existing world is cleared by `ensureBuilt` (`clearLegacyArena`, found by lava right outside its old edge) and rebuilt at the current size, keeping the altar; tested from the radius 54 arena in the dev world (old floor, walls, lid and exit all gone, new walls in place; the 18 to 54 step was tested before); waves are now `WAVE_PAUSE_TICKS` 120 (6 seconds) apart. A solid wall of lava (Warfist's idea, 23 October 2026) surrounds the disc right up against its edge: `LAVA_WALL_THICKNESS` 4 blocks, from `LAVA_WALL_BELOW_FLOOR` 6 below the floor to `LAVA_WALL_ABOVE_FLOOR` 100 above it (raised from 16 at Warfist's request so you feel stuck in a tall pillar of lava; about 41,000 lava blocks, its top at y 164 well past the 64-block fog, so it fades out of sight), built by `ensureBuilt` if its marker block (at the top of the wall) is missing (so existing arenas get it, and a shorter older wall is rebuilt taller). Fluids never flow in this dimension (`mixin/FluidSpreadMixin` cancels `FlowingFluid.spread` there), so it stays a wall and doesn't pour onto the floor or into the void; it also glows, lighting the arena orange near the edge. Touching it hurts and sets you on fire, as lava does. Checked in game: the count of lava blocks was the same 600 ticks later, the floor edge stayed netherrack, and the wall looks right from inside. Fire on the altar netherrack (checked every 10 ticks) is the fight trigger: `onAltarLit` prints "The altar burns..." and spawns **Herobrine, The Chaos King** in the fire on top of the netherrack (`BOSS_SPAWN`, one at a time). **The fight** (`entity/ChaosKing`, Warfist's design, 23 October 2026): *Rising* (a visual lightning storm of 12 bolts, he says a line, floats up 5 blocks, invulnerable from the start) → *Waves* (he hovers with no AI; 4 waves, each called when the last is dead: 5 strays + 5 husks per player; then per player 1 ravager, 2 evokers, 3 vindicators; then per player 4 guardians and 3 drowned; then 1 warden with 25% of its health per player (125 for one player); wave sizes use the players in the arena) → *Descending* (invulnerability off, flies fast (`DESCENT_SPEED` 0.4 blocks a tick; Warfist found the first 0.05 "wayyyy too slow") down onto the altar's top, `BOSS_LANDING`, where he appeared; 13 ticks from 5 blocks up) → *Fighting* (melee; a Wither Skull, Fire Charge or Lightning Strike every 12 s (was 30), `ABILITY_COOLDOWN_TICKS`, never the same twice running, first after 10 s; Warden health 500; he also fights players' army units). His units carry `ChaosKing.MINION_TAG` and an extra target goal for players' units; the Guardians swim in air like Guardian units (`UnitWaterMixin` also covers the tag) and the Warden's dig cooldown is refreshed so it doesn't burrow. His attacks use the crowns' own code made generic (`WitherSkullAbility.shoot`, `FireChargeAbility.shoot`, `LightningStrikeAbility.strike` take any `LivingEntity`), so they never break blocks. Beaten (`die`), he drops `SOULS_DROPPED` (1) Soul of Chaos and not his crown, and `ChaosArena.onBossDefeated` turns the altar's fire to soul fire (`mixin/SoulFireMixin` lets soul fire survive on that netherrack only: vanilla removes it at once). `ChaosArena.checkSoulFire` (every tick) turns Netherite Upgrade templates thrown into the soul fire into Chaos Upgrades (the converted items are made invulnerable, since other items burn up in the fire). **His words are banners** (Warfist, 23 October 2026): `ChaosKing.say` sends a title (dark red, bold, short because titles are huge and clip on small windows) and a subtitle to every player in the arena, not chat messages; the lines are mine and placeholders. **His legion never fights itself** (Warfist saw the wave units shooting each other): `ChaosKing.isLegion` (the boss, `MINION_TAG` units, and `ALLY_TAG` things they make, the Evokers' vexes) makes them allied to each other in `EntityAlliesMixin` (so no retaliation) and `ChaosArena` denies damage between them through `ALLOW_DAMAGE` (arrows, fangs, beams, blasts and bites alike); they still attack players and players' army units. Tested: a legion stray and husk are allied, a melee hit and an arrow from a legion stray both did nothing to a legion husk, while plain mobs still fight. **Timing and wipes** (Warfist, 23 October 2026): the first wave comes `FIRST_WAVE_DELAY_TICKS` (300, 15 s) after he finishes rising, with a banner ("RUN WHILE YOU CAN") so players can back away from the altar; later waves come `WAVE_PAUSE_TICKS` (160, 8 s) after the last is dead, and he comes down `LAST_WAVE_PAUSE_TICKS` (80) after the final wave (measured in game: 300 and 160 ticks). When no living player is left in the arena (all dead, logged out, or gone from the dimension) `ChaosArena.resetFight` removes him, his legion and the altar's fire (a soul fire from a won fight stays), so the altar must be lit again; it runs from the boss's own tick every second, and at once on a player's death or logout (`resetIfEmpty`); there's no Fabric event for changing dimension in this version, so that case waits up to a second for the boss's tick. Tested: with one player killed, the boss, 6 units and the fire were gone and he logged the reset. His state (stage, wave, hover height, cooldown) is saved. Name "Herobrine, The Chaos King". Tested in game (Normal, survival, one player, with a script that killed each wave): the altar spawns him in the fire, 20+ bolts, he rises to 5 blocks above, invulnerable through all four waves (a 50-damage hit did nothing), exact wave compositions, Warden at 125 health, descent and landing, 6 ability uses (all three kinds, no repeats), the Soul of Chaos dropped (1, no crown), soul fire stayed, 3 Netherite Upgrades became 3 Chaos Upgrades. Not tested: real combat or balance (health, damage, speed, wave difficulty are untuned), more than one player, the lines he says (mine, placeholders), his front and the arms' width, the fight after a server restart, a rematch (Warfist confirmed on 25 October 2026 that relighting the altar after a win and reloading mid-fight both work). Tested in game: the dimension loads into an existing world (data-defined dimensions do), the arena builds, the look, and lighting the altar. **Decision (Warfist, 23 October 2026): no mobs spawn naturally in the arena and there is no treasure; the only things in it are what the boss and its summons are, and what the boss drops when beaten.** The biome (`chaos_void`) has empty spawner lists and no features, and the dimension has no structures, so nothing generates; checked in game on Hard in survival for 90 seconds: no mobs, items or other entities appeared. Don't add spawners, chests or structures to it. **The portal** (23 October 2026, Warfist tested it and it works): a Music Disc 11 (first said as 13, corrected) thrown into a nether portal (`mixin/NetherPortalMixin`) calls `portal/GlitchedPortals.convert`: the swirl becomes `ModBlocks.NULL_PORTAL` (`block/NullPortalBlock`, a `Portal` that sends players to `ChaosArena.ARRIVAL` and stores where they came from in the `ORIGIN` attachment; uses vanilla's missing-texture look, not a drawn texture) and the obsidian frame becomes random blocks (`FRAME_BLOCKS`), recorded in the `GlitchedPortals` SavedData; `mixin/FallingBlockMixin` stops recorded blocks falling, and breaking a null block (`playerWillDestroy`) clears the whole portal. Block changes use `UPDATE_CLIENTS` only: shape updates still run, so don't place vanilla portal blocks that way. The arena's exit (`ModBlocks.NULL_EXIT`, unbreakable, bedrock frame at `ChaosArena.EXIT_PORTAL`) is built by `onBossDefeated`, removed when the altar is lit and by `ensureBuilt` if there is no soul fire. Sounds: `portalactivationsound` (`ModSounds.PORTAL_ACTIVATE`, plays on conversion) and the boss music "Empire" by Laudividni (`ModSounds.BOSS_MUSIC`, looped client-side by `client/BossMusicClient`, started and stopped by `BossMusicPayload` from `ChaosArena.updateMusic`; credit in `CREDITS.md`, `fabric.mod.json` and the README, and the track is not under the mod's CC0 license). Source MP3s are in `sounds/`; the game uses the `.ogg` conversions in `assets/crownofchaos/sounds/`. The dimension was darkened (ambient light 0.07 after a too-dark 0.04, fog 8 to 96 for the bigger arena). The return trip works (Warfist tested it). Disc 11 only glitches portals in the Overworld (`GlitchedPortals.convert`), so nobody is trapped from the Nether. **Fixes after Warfist's full survival test (23 October 2026):** nobody can hurt their own faction's units (`UnitManager.initialize` ALLOW_DAMAGE); `mixin/ShulkerMixin` stops unit and legion shulkers leaving wild copies behind when a bullet hits them (vanilla's `hitByShulkerBullet`); `mixin/LegionDropsMixin` makes the legion drop experience but no items or gear; the Chaos King is kept `EDGE_MARGIN` (2) blocks in from the floor's edge and put back on the altar if he falls (`ChaosKing.keepInArena`), with lava and fire pathfinding blocked; a bedrock shell (ring, floor under the lava and a lid at the top, `ChaosArena.buildBedrockShell`, marker `BEDROCK_SHELL_MARKER`) seals the lava wall; a "WAVE n CLEARED" banner follows waves 1 to 3; when the last wave dies he gets a diamond sword with its attribute modifiers emptied and the glint forced on (`drawSword`: visible only, no extra damage; not yet looked at in game). Tested in game: own-unit damage refused, legion drops none (a plain zombie drops), the clamp, the shell. Not reproduced: Drowned units "dying out of water" (a Drowned unit lived 1,400 ticks both in the arena and on land in daylight); ask Warfist for details if it comes up again. Warfist later called the Drowned deaths a fluke. A shulker unit's bullets used to hit anyone in their path and give them Levitation, so `mixin/ProjectileMixin` now makes them fly through everyone the unit is not fighting (not tested in game). **After Warfist and his brother tested online (24 October 2026):** a crown you wear now counts at the Commander's Table when its crown slot is empty (`CommanderTableMenu.effectiveCrown`, lists its units from the moment it opens; tested with a worn Crown of Void: 2 units, none without a crown); the Crown of Void speeds up elytra flight (`client/VoidElytraClient`, `CrownEffects.VOID_CROWN_ELYTRA_SPEED_BONUS` per tick up to `VOID_CROWN_ELYTRA_MAX_SPEED` blocks a tick, strengthened on 24 October 2026 from 1.015 and 2.2 to 1.03 and 3.0 (60 blocks a second) plus a cruise floor `VOID_CROWN_ELYTRA_CRUISE_SPEED` of 1.4 (28 blocks a second) for a glider already moving, because Warfist could not feel the first version (the strengthened one he called perfection, 25 October 2026: leave it), applied on the client because a player's own movement is theirs; tested: a 20 degree glide reached 2.2 in about 60 ticks against 1.6 after 100 without the crown). Wave sizes do scale with the players in the arena when each wave is called (tested with a fake second player: 20 minions in wave 1 against 10 for one player), so "the Warden scaled but the mobs didn't" most likely means the second player arrived after waves 1 to 3 were called; ask before changing that. **Advancements** (tab renamed "The Crown of Chaos" with the Crown of Darkness as its icon at Warfist's request, so the last advancement became "Crowned in Chaos" to avoid two with the same name; Warfist, 24 October 2026: "achievements for getting all the major items"; which items and the extra milestones were my reading, so ask if he wants others): a tab of 21 under `data/crownofchaos/advancement/` (not `recipes/`, which holds the hidden recipe unlocks), generated from a script (root `root` = Golden Crown, background vanilla's `end` texture; the nine crowns; `all_crowns` needs all nine criteria at any time; `sculk_cord`, `ankh_charm`; `netherite_any` and `netherite_all`; the chain `incomplete_crown_of_chaos` -> `enter_arena` (changed_dimension) -> `defeat_chaos_king` (player_killed_entity, predicate key `minecraft:entity_type`) -> `soul_of_chaos` -> `chaos_smithing_template` -> `crown_of_chaos`). Goals and the challenge announce in chat. Titles and descriptions are in `en_us.json` under `advancements.crownofchaos.*`; some titles are Warfist's own item lines ("Fit for a King", "Ra Would Be Proud"). Tested in game: all 21 load, and holding a Golden Crown, Crown of Void and Netherite Crown of Iron awarded exactly the root, the Void crown and `netherite_any`. Warfist confirmed in play (25 October 2026) that the advancements all work. **The Chaos King's laugh** (Warfist's `sounds/evil-laugh.mp3`, converted to `assets/crownofchaos/sounds/chaos_king_laugh.ogg`, 3.2 s; `ModSounds.CHAOS_KING_LAUGH`, 24 October 2026): `ChaosKing.laugh` (sent as a sound packet to every player in the arena, placed 2 blocks from each toward him at volume 1.5, so it is as loud for the far side of the arena as for the nearest; a plain world sound fades out with distance, `LAUGH_COOLDOWN_TICKS` 80 so one moment is one laugh) is called when a wave starts (`callWave`), when a player dies in the arena (`ChaosArena` AFTER_DEATH, before a wipe clears him) and when an ability hurts a non-legion target (AFTER_DAMAGE, damage credited to him with a direct entity that isn't him, so skull, fireball and lightning count and melee doesn't). Tested in game by logging each call: wave start, a forced lightning strike and a player death all laughed. Warfist heard the laugh, the wail and the victory music in play (25 October 2026) and confirmed them. Warfist (25 October 2026): the laugh and the death wail are royalty free and need no credit. **Victory banner** (24 October 2026): `ChaosArena.onBossDefeated` sends every player in the arena a title "VICTORY" (gold, bold) with the subtitle "The Chaos King has fallen." (yellow), shown for 6 seconds (`VICTORY_BANNER_*`); my wording; seen in a screenshot, it fills the middle of the screen. **Victory music** (Warfist's `sounds/Celebration - by Laudividni & ft. @TenM.mp3`, converted to `victory_music.ogg`, 2:11; `ModSounds.VICTORY_MUSIC`, 24 October 2026): `BossMusicPayload` now carries a `Mode` (NONE, FIGHT, VICTORY) instead of a boolean; `ChaosArena.onBossDefeated` sends VICTORY to everyone in the arena, which replaces the looping fight music on their client (`BossMusicClient`, played once, and it stops if they leave the arena dimension), and drops them from `musicPlayers` so `updateMusic` doesn't stop it a second later. Warfist confirmed it works. Credits updated in `CREDITS.md`, `fabric.mod.json` and the README: Laudividni ft. @TenM, with Laudividni's channel link; Warfist (25 October 2026): the artist's credit and the mention of TenM are all that is required, no further link. Heard in play and confirmed. **His death wail** (Warfist's `sounds/death-wail.mp3`, converted to `chaos_king_death.ogg`, 2.5 s; `ModSounds.CHAOS_KING_DEATH`): played to every player in the arena in `ChaosKing.die`, just before `onBossDefeated`, through the same `playToEveryone` as the laugh and without the laugh's wait. Tested in game only as far as: killing him ran the death path with no errors and the soul fire rose; the wail was heard in play and confirmed by Warfist (25 October 2026). **Faction Flag as a real banner** (Warfist, 24 October 2026: it did not behave like a normal banner): `FactionFlagItem` now extends `BannerItem` (on the white banner's blocks, which it never places; `ColorCollection` is how 26.2 holds the banner blocks, `Blocks.BANNER.pick(color)`), because the loom only takes `BannerItem`s. Patterns added at a loom stay on the stack, `useOn` places the faction-colored banner from that stack so the block entity gets them, and `FactionManager.breakFlag` copies the banner's patterns onto the flag it drops. The loom's own preview draws the base white (it reads the item's block), but the result keeps the faction color. Tested: the loom accepts it, a patterned flag places as an orange banner with its pattern, and the dropped flag keeps it. Warfist confirmed in a real loom screen (25 October 2026) that it works. **Faction menu showing "create" after a re-log** (Warfist, 24 October 2026; the server still knew the faction): cause not pinned down, so the client now asks for the factions itself: `FactionActionPayload.Action.SYNC` (answered by `FactionManager.sendTo`), sent by `FactionClientState` when it joins and by `FactionKeybind` before opening the menu (the open menu refreshes when the answer arrives), plus the server resends to a joining player after `RESYNC_DELAY_TICKS` (40). Tested: with the client's list wiped, a SYNC request restored it. Warfist confirmed in play (24 October 2026) that the menu issue is fixed, and that the boss's sword shows and the shulker units' bullets no longer levitate him. The Crown of Poseidon also grants Regeneration I while its wearer is in water (`CrownEffects.POSEIDON_WATER_REGENERATION`, added and removed each check like the terrain Swiftness; tested: on in water, off when dry; Warfist confirmed in a real pond on 25 October 2026 that it switches off after leaving the water). **Wave size is locked when the altar is lit** (Warfist's idea, 24 October 2026; I first locked it at wave 1 and he preferred the altar): `ChaosKing.fightPlayers` is the number of living players in the arena when he spawns (his first tick), saved with him, and every wave including the Warden uses it, even if someone dies or joins later; `callWave` still sets it if it is somehow 0. **Latecomers** (Warfist, 24 October 2026): `ChaosKing.catchUpLatecomers` (every second while waves run) raises `fightPlayers` when more living players are in the arena than it holds, adds their share of units to the wave in progress (`spawnWaveUnits`) or of health to the Warden, and shows a "MORE FOOLS ARRIVE" banner; it never lowers the count. Tested with a fake second player after wave 1: 10 more units appeared. (The test world had 7 leftover legion units from an earlier test, which stayed counted.) Not tested in game. Not tested: real multiplayer fights. **The lore book** (Warfist, 26 October 2026: a book players spawn with, hints in riddles about the old Herobrine lore, in the voice of a former crown-bearer, scrambled words and pages; he approved the tone, scrambling over block characters, the title "A Warning from a Former King", and a rewritten page 9): `ModItems.LORE_BOOK` (`item/LoreBookItem`) opens `client/LoreBookScreen` (a vanilla-like single-page book drawn in the faction menu's teal; arrows, A/D, page keys and the wheel turn pages) through `LoreBookPayload`, which carries a bitmask of unlocked pages built by `lore/LoreBook` from the player's advancements when they use it (so a replacement book is always right). Eleven pages in `en_us.json` as `lore.crownofchaos.page.N`, glitched words marked `[[like this]]` and drawn with the obfuscated style, locked pages scrambled whole; pages 1-3 are always readable, then `root` (4), `all_crowns` (5), `incomplete_crown_of_chaos` (6), `enter_arena` (7, 8), `defeat_chaos_king` (9), `chaos_smithing_template` (10), `crown_of_chaos` (11). Given once per player the first time they have a Golden Crown (Warfist, 26 October 2026: it follows the lore; first it was given on join): `LoreBook.initialize` checks every 40 ticks whether a player without the `lore_book_given` attachment (copied on death) has the `root` advancement, so it also reaches players who had a crown already. A Golden Crown plus a vanilla book craft another (shapeless, the crown is used up; recipe advancement keyed to the Golden Crown). The item's sprite is Warfist's `Sprites/Lore-book.aseprite` (32x32, a teal book with glitch marks), exported to `Sprites/Lore-book.png` and `textures/item/lore_book.png` (the aseprite had no PNG, so it was read directly; the vanilla `Book_JE2_BE2.png` that was also in `Sprites/` was only his reference and is unused). Tested in game: the book arrives about two seconds after a player first has a Golden Crown and not before (and only once), the screen opens from the server, an unlocked page and a locked page look right in screenshots. Warfist confirmed on 26 October 2026 that the recipe works and that the pages unlock as advancements are earned. **War round 1 changes** (Warfist and his brother Quinn tested war with flags and every command, 28 October 2026; his four takeaways): (1) a sentry now chases an *enemy combatant* (`UnitManager.isEnemyCombatant`: a player or unit that is at war, or in the faction's territory and Targeted, or not Allowed on red alert) for as long as it stays in the faction's territory, however far from the post (before: a 16-block `SENTRY_LEASH`, which still applies to wild mobs and when the post is outside the territory), then returns to its post; (2) `UnitGoals.WatchGoal` (every 10 ticks) makes units drop a wild-mob target for an enemy combatant they can see (`findPriorityTarget`, `rank`); (3) what the owner attacked recently now also counts for players and units, not only mobs (`isOwnerTarget`, still not Allowed factions or pets), and is prioritised the same way; (4) a new order **Hunt** (`UnitOrder.HUNT`, `UnitCommandPayload.Command.HUNT`, overlay key 8 so the older number keys did not move): sentry behaviour plus seeking combatants anywhere in the territory without needing to see them (`HUNT_SEARCH_RANGE` 128) with a transient follow-range bonus (`HUNT_FOLLOW_RANGE_BONUS` 100, `UnitManager.refreshOrderEffects`) so the vanilla targeting and path finding reach far. Tested in game in the Chaos Arena with a faction, a flag with a banner (the server drops claims whose banner is missing, which fooled my first test), a war and unit stand-ins for enemies: a hunter 60 blocks from an enemy it could not notice acquired it and walked steadily to it, then dropped it and went back when the territory was removed; a sentry fighting a wild zombie switched to an enemy combatant that came into view; a free unit joined in on a zombie the owner hit. Not tested with real players or the real flag. **Units on Peaceful** (Warfist, 25 October 2026: so players can use units without fear of other hostile mobs): vanilla refuses to create hostile mobs on Peaceful (`EntityType.create` checks `canSpawn`, so unit items failed and saved units would not load back) and removes the ones that exist (`Mob.checkDespawn`). `mixin/EntityTypeMixin` lets the server create them while `UnitPeaceful.summoning` is on (the unit item) or when loading a saved entity of a unit type (`UnitPeaceful.isUnitType`), and lets the client build whatever the server sends; `mixin/MobDespawnMixin` stops units being removed; `UnitItem` checks `isEnabled` instead of `canSpawn`. Plain hostile mobs are still refused on Peaceful. Tested in game on Peaceful: a Husk unit summoned, saved and loaded back, the client saw it, it lived 200 ticks, and a plain zombie was refused to create and to load. Vanilla limits remain: Peaceful stops mobs attacking players and Shulker units don't shoot on Peaceful. **Menu palette** (Warfist, 25 October 2026: the mod's theme is the Crown of Darkness): the faction menu, Commander's Table screen and unit command overlay changed from brown and gold to the crown's teal and cyan, taken from its sprite (`Sprites/CrownOfDark.png`: dark teal `#05404f`, teal `#079093`/`#076d72`, cyan `#35e9f5`); the old `GOLD` constants are now `ACCENT` and `ACCENT_DARK` in `FactionScreen` (and `CommanderTableScreen`), and red and green stay for status. Seen in screenshots of the faction menu (both states) and the Commander's Table. **The mod version stays 1.0.0 until the first official release** (Warfist, 25 October 2026): don't bump it or tag anything until he says the release is ready. The idea it was waiting on was the lore book, which is done (26 October 2026). **Decision (Warfist, 25 October 2026): no rules inside the arena** (placing, breaking, pearls, beds, death all stay vanilla), so people can fight the boss creatively; don't add any without asking. **Chaos smithing template** (`ModItems.CHAOS_SMITHING_TEMPLATE`, "Chaos Upgrade", 23 October 2026): a vanilla `SmithingTemplateItem` (epic; applies to the Incomplete Crown of Chaos, ingredient the Soul of Chaos; empty-slot icons are vanilla's own `container/slot/helmet` and `container/slot/ingot` sprites, referenced not copied), sprite Warfist's `Sprites/chaossmithingtemplate.png`. The smithing recipe exists (`data/crownofchaos/recipe/crown_of_chaos_smithing.json`: Chaos Upgrade + Incomplete Crown of Chaos + Soul of Chaos = Crown of Chaos; tested with a smithing menu: all three give the crown, a missing Soul or the netherite template gives nothing). No recipe to get the template, and that is final (Warfist, 25 October 2026): the only way to get one is throwing a Netherite Upgrade smithing template into the soul fire on the altar, which is how `checkSoulFire` makes it. Creative tab only. Tooltip and icon checked in game. **Soul of Chaos** (`ModItems.SOUL_OF_CHAOS`, 19 October 2026): the boss's drop, a plain
epic, fire-resistant material used only for smithing the Crown of Chaos; sprite is Warfist's
`Sprites/SoulOfChaos.png` (32x32), no description yet. Only in the creative tab until the boss exists.
Every crown now has a custom 3D model, used both worn and as the item (last commit
`edbbefa`). Only the magical crowns always shimmer: the Golden Crown has no forced glint
(it shimmers only when enchanted), and worn models copy the real item's glint.

### Crown 3D models

Warfist (and Ted) draw them in Blockbench and save them to `3dmodels/`; Warfist often
re-saves a model several times in a row, so after "I updated the model" diff the new export
against the worn model and bring in only what changed (pieces and texture).

- **Entity models** (Under, Poseidon, Nether, Wither): Blockbench "Modded Entity" exports
  converted to classes in `client/model/` (`UnderCrownModel` and so on), worn through
  `CrownArmorRenderer`, and drawn as items by `CrownSpecialRenderer`, registered as the
  special item model type `crownofchaos:crown` (`items/<crown>.json`, base model
  `models/item/<crown>_3d.json` for the display transforms).
- **Java Item models** (Golden, Darkness, Sun, Winter, Iron, Void): `3dmodels/<Name>.json`
  plus `.bbmodel`. Each becomes `models/item/<crown>_worn.json` (the export's elements with
  `groups` dropped, textures renamed to `crownofchaos:item/<crown>_model`, taken from the
  `.bbmodel` in order) and is worn through `CrownItemModelArmorRenderer` via the stand-in item
  `items/<crown>_worn.json`. As items, `items/<crown>.json` (and the netherite one) point at
  the same worn model, whose parent `models/item/crown_display.json` holds the inventory,
  hand, ground and frame transforms.
- **Converting an export:** drop the untextured `reference head` cube (its `#missing` faces
  stop the model from loading). That cube (9 pixels, 3.5 to 12.5, from y=0) is the head with
  its hat layer, so `display.head` translation `[0, 5.6, 0]`, scale `1.6` puts the crown on
  the head exactly as drawn. Blockbench exports 90 and 180 degree turns as `x`/`y`/`z`
  rotations, which 26.2 reads fine; the export's UVs are right even with several textures of
  different sizes.
- **Per-crown head transforms** (bands drawn 12 pixels across look too wide at full size, so
  shrink crowns that hug the head, keeping the part that rests on the head in place):
  Darkness, Sun: 1.6 / 5.6. Golden, Winter (sit on top): 1.44 / 5.28 (Warfist asked for the
  Golden Crown smaller). Iron, Void (band hangs down the sides): 1.3 / 5.9. Void also has a
  `[0, 180, 0]` rotation in every display entry because it was drawn facing backward (Enderman
  eyes on the back).
- The Crown of Void uses three textures (`crown_of_void_model`, `_tines`, `_frame`). The
  Crown of Winter's two-color yellow and red texture is deliberate (cartoony, like the Ice
  King from Adventure Time), not a placeholder.
- The hand-drawn sprites stay in `Sprites/` and `textures/item/` as artwork; nothing uses
  them as item icons now.

**Seen in game:** every crown worn (front view), all ten in the inventory (Winter still as a
sprite at the time), the Crown of Sun held in third person. **Not seen yet:** the
entity-model crowns (Under, Poseidon, Nether, Wither) held in hand, the Winter crown's 3D
inventory icon, crowns in item frames, on shelves and dropped on the ground. The
`crown_display` transforms (gui scale 0.8, rotation 30/225) are a first pass; tweak them if
Warfist asks.

### Mod compatibility

Warfist tests the built jar in a Modrinth profile, "Riley & Alexys Pack 1.0.0"
(`~/AppData/Roaming/ModrinthApp/profiles/`, ~85 mods including Create, Sodium, Iris, Entity
Culling). Findings (7 October 2026):
- **Create** (`create-fly`, with Flywheel) drops the glowing outline of block displays, even
  server-side ones; mob outlines still draw. Ore Sense's markers were glowing block displays,
  so no ores showed there; they are now invisible glowing client-only shulkers
  (`OreSenseClient.OreMarker`: not pickable, not collidable, color from `getTeamColor`).
  Don't go back to block displays for glow effects.
- **Better Combat** (through PlayerAnimationLib) draws your own body in first person during
  attacks and hides vanilla helmets then, but not the crowns, which have their own renderers,
  so they covered the screen. `client/model/FirstPersonCrowns.hide` skips a worn crown while
  PlayerAnimationLib's `playerAnimLib$isFirstPersonPass` flag is set on the render state (read
  by reflection, no dependency) or the wearer's head part is hidden; both crown renderers call
  it. Third person, other players and the inventory preview are separate renders and still
  show the crown. Checked in game with the library loaded (the flag hides it, normal renders
  don't); Warfist confirmed in his pack that swinging no longer covers the screen (7 October
  2026). A test can't drive a real swing (the window isn't focused).
- **Lootr** (loot chests that give each player their own loot) turned out not to be the culprit
  for a missing Ankh Charm: the real bug was in `ModItems.isAnkhCharmChest`, which asked for a
  pyramid *piece* at the chest, but the hidden chambers sit about 11 blocks below the pyramid's
  recorded bounding box, so no Ankh Charm ever generated (vanilla chests too). It now matches the
  pyramid by its footprint, ignoring height (`pyramidAround`). Checked in a world made at the
  time (seed 12345, a real pyramid found with `findNearestMapStructure`): one of the four chests
  gets the charm, with and without Lootr 26.2's chests (Lootr fills chests through
  `LootTable.fill`, so the loot event still fires). `/place structure` pyramids don't register
  as structures, so they can't test this. Not played through with a real Lootr chest opened by a
  player.
- To reproduce a pack problem, copy the profile's mod jars into `run/mods` and narrow them down
  by halves (keep each mod's dependencies). In the dev client, Forge Config API Port and
  Traveler's Backpack crash on a night-config version clash that the real launcher doesn't
  have; leave them out. Remove `run/mods` and the configs the mods write to `run/config`
  afterward.
- When testing ore outlines, stand in a sealed pocket in solid rock (not spectator mode, which
  turns off culling), so hidden-block culling is exercised.

Open ideas from `developing plans/Crown Ideas.md` (Warfist's planning file, where he throws
thoughts to come back to; it changes often, so reread it, implement only what Warfist asks for,
and don't tidy or update it: he said on 25 October 2026 there is nothing to do about it):

- The icicle looks like a thin shard of Packed Ice (`IcicleEntity.LOOKS_LIKE`) as a stand-in.
  When the mod moves to Minecraft 26.4, which adds a vanilla icicle block, point it at that
  block; never copy Mojang's model or texture into the mod.
- The **Crown of Chaos**: crafted from all nine crowns into an unusable "incomplete" crown,
  completed with a boss drop and a smithing template; combines the immunities and the
  teleport dodge; abilities "The pain of death" (volley, wither skull, fire charge and
  lightning at once) and "Death's door" (Darkness Pulse lasting 300% longer); netherite
  stats from the start and no netherite upgrade; "glitched" combined texture.
- Faction system testing that needs two players (see below).

## Faction system (beta)

Warfist's full plan is under "factions system" in `developing plans/Crown Ideas.md`. It's
built in parts and is labeled **beta** in the README: Warfist moved it from alpha to beta on
5 October 2026, once the main features were complete. Keep that label until he says otherwise. **Part 1 (factions, menu, flags and territory)** and **part 2 (the
Commander's Table and unit items)** are done and committed (4 October 2026).

**Part 2, the Commander's Table:** a block (`block/CommanderTableBlock`, Warfist's 3D model
from `3dmodels/Commander-table.json`, turns to face the placer) whose menu
(`menu/CommanderTableMenu`, screen `client/menu/CommanderTableScreen`, written by a helper
agent) works like a stonecutter: a crown slot, a list of that crown's units, two cost slots
(either order) and a result slot. It keeps no items. Units are defined in `unit/ModUnits`:
18 units for 8 crowns, from Warfist's plan plus Enderman and Shulker for the Crown of Void and the Drowned (always summoned with a trident that never drops, `UnitManager.makeUnit`) for Poseidon,
which he added later (the Golden Crown and Under Crown have none). Warfist fills in the
costs in `developing plans/Unit Recipes.md`; they're set in `ModUnits` (the README lists
them). Each unit costs exactly two kinds of item; a cost can be any item with a tag (the Polar
Bear takes any wool). Warfist's "seaweed" for the Guardian was read as Kelp. Unit items
(`item/UnitItem`) use the vanilla spawn egg models.

**Part 3, summoning units** (done 4-5 October 2026): using a unit item on a block summons
the mob as a unit of the player's faction (they must be in one). `unit/UnitManager` holds:
- the `crownofchaos:unit` data attachment (`UnitData`: owner, faction, unit id, faction
  color), saved with the mob and synced to every player;
- the fighting rules, decided by Warfist: units are **passive** (don't attack, don't fight
  back) except against wild hostile mobs (anything that is an `Enemy`) that are attacking their owner or their faction's units
  (targeting them, or hurt the owner in the last 5 seconds; this holds while following, until the follower is
  `FOLLOW_FIGHT_RANGE` (40) blocks from its leader), or any within range while on the Attack order
  (added 5 October 2026 at Warfist's request), against factions at war with theirs (`atWar`, a stub until war exists) and,
  inside their own territory, against players and units of Targeted factions or, on red
  alert, anyone not Allowed. They never fight their own faction. This is applied by making
  units count everyone else as allied (`mixin/EntityAlliesMixin` on `Entity.isAlliedTo`,
  which all vanilla targeting checks), plus an extra target goal so goal-based units attack
  enemies. Brain-based units (Piglins, Hoglins, Wardens) ignore goals, so `UnitGoals.WatchGoal`
  finds enemies for them and hands them over: Wardens through their anger (`increaseAngerAt`),
  Piglins through `ANGRY_AT` plus `ATTACK_TARGET` (they drop any target they wouldn't pick
  themselves), Hoglins through `ATTACK_TARGET`.
- The faction color is client-side and deliberately loud (Warfist: clarity over looking like the
  dye): `client/mixin/LivingEntityRendererMixin` multiplies the model color by a vivid color from
  `client/UnitColors` (hand-picked per dye color; white leaves the mob unchanged), layers aren't
  tinted, and `client/mixin/UnitOutlineMixin` plus `MinecraftGlowMixin` give every unit within 64
  blocks (`UnitClient.OUTLINE_RANGE`) an outline in that color while the player has line of sight (`hasLineOfSight`, cached per tick;
  outlines otherwise show through walls); selected units are outlined white, through walls too. Dark mob textures stay dark under a tint, which is why the outline exists.
- Units also fight any mob their owner hit in the last `OWNER_ATTACK_TICKS` (200, not players,
  not the owner's pets, not Allowed factions' units; `UnitManager.isOwnerTarget`), then the old
  order resumes by itself (target dropped by `WatchGoal`, `OrdersGoal` takes over). Warfist
  asked for this 8 October 2026; a unit and a mob in line of sight tested in game.
- Vexes summoned by an Evoker unit become minions (`UnitManager.adoptSummons`, unit id
  `minion`, not in `ModUnits`): faction tint, fighting rules and unit goals, but not commandable,
  not reclaimable and not counted by `countUnits`.
- A Riptide spin attack (the Crown of Poseidon's ability, or a trident) stops dead and bounces back on
  the first living thing it touches, and Follow teleports units right onto their owner, so units
  blocked the dash (Warfist, 16 October 2026). `mixin/SpinAttackMixin` drops the player's own and
  faction's units from the list of things the spin attack touches (`UnitManager.isOwnFactionUnit`,
  on both sides; the client gets the player's faction from `UnitManager.clientFaction`, set in
  `UnitClient`). It doesn't stop other players hitting those units, and a player can still hit their own
  units any other way. Tested on the server by calling the spin attack check: own unit passes, wild cow
  stops it. Not tested: a real dash in water.
- Each player can have at most `UnitManager.MAX_UNITS_PER_PLAYER` (40, set by Warfist) units; `UnitItem` refuses
  to summon past it (counted over loaded chunks by `UnitManager.countUnits`).
- Guardian units treat air as water (`mixin/UnitWaterMixin` makes `Entity.isInWater` true for them, so
  they use their underwater movement, don't flop and don't change sounds; `UnitManager.setUpUnit` swaps their
  water-only navigation for a `FlyingPathNavigation`, since a Guardian's own one finds no path through air).
  Tested in game on dry land: they follow, and shoot their beam at a target; they float a little off the
  ground while moving and settle on it when idle.
- **Crown passivity** (Warfist reversed his 12 October decision on 30 October 2026, after testing in single
  player and finding his crowns did not do it: "all the crowns have the hostile mobs that match the units
  they can create be passive to the player"): `item/CrownPassivity.isPassiveTo`, called from
  `mixin/EntityAlliesMixin` (so every vanilla targeting check, brain mobs included, treats the wearer as an
  ally), makes a mob whose type a crown can make a unit of leave its wearer alone (a netherite crown counts, and
  the Crown of Chaos covers every unit type; the Golden and Under crowns have no units). My choices: a mob the
  wearer hurt fights back for `PROVOKED_TICKS` (200, 10 s), and the Chaos King's legion and army units are not
  covered, so the boss fight and the unit rules are unchanged. Tested in game on Hard-ish survival: with the
  crown a Husk, Stray, Wither Skeleton, Pillager, Vindicator, Piglin, Hoglin, Shulker (and a Husk with the
  Crown of Chaos) ignored the player 8 blocks away while the same mob without the crown targeted them, a Zombie
  stayed hostile to a Crown of Sun wearer, and a hit Husk then targeted its attacker. The Warden was not
  conclusive (it did not notice the player without a crown either in the time tested), Endermen and
  Guardians were not tested.
- Units never despawn and don't burn in daylight (`mixin/MobSunMixin`); Piglin and Hoglin units
  don't zombify. Warden units never give Darkness (`mixin/WardenDarknessMixin`) and always keep
  a dig cooldown, because a Warden with none digs into the ground and is removed (Warfist:
  Warden units always stay above ground).

**Part 4, unit commands** (done 5 October 2026), in `unit/UnitCommands` with orders in
`UnitOrder` (FREE, SENTRY, FOLLOW, ATTACK) saved in `UnitData` (with the sentry post and the
follow leader). The selection scheme was Claude's suggestion, accepted by Warfist:
- Players command their faction's units whose crown they wear (netherite counts).
  Right-click toggles sentry; sneak + right-click toggles selection (selected units glow for
  that player only: `UnitSelectionPayload` and `client/mixin/MinecraftGlowMixin`).
- Orders (`UnitCommandPayload`) go to the selection, or with none, to all commandable units
  within 32 blocks. Keys: V toggles `client/faction/UnitCommandOverlay` (it stays open after an order; a slim see-through HUD row above the hotbar and
  status bars, with small status and hint text above it, shrunk to fit narrow screens; not a screen, so camera and movement stay free; `UnitClient` takes the number keys 1-7
  before vanilla's hotbar does and gives the orders), B toggles follow (`client/UnitClient`).
- `UnitGoals.OrdersGoal` moves units for sentry and follow (brain mobs via their walk
  target, flying mobs via move control); `UnitGoals.WatchGoal` drops targets the rules no
  longer allow. Attack makes other factions' players and units within 24 blocks hostile
  anywhere (not Allowed ones).
- Reclaim turns units back into items (the plan's "war off" condition is moot until war
  exists). Owners are told when a unit dies out of sight (within 64 blocks, 70 degrees of
  their view, and not blocked).
- Brain memories must be checked with `checkMemory(..., REGISTERED)` before reading or
  erasing: most mobs don't have them, and reading one throws (this crashed a test once).

The table's recipe is Warfist's (map, 2 diamonds, Faction Coin, 3 planks); the Faction Coin
is 8 gold ingots around an emerald.

**Decisions Warfist made** (don't revisit without asking):
- Faction colors are the **16 dye colors**, one faction per color (so at most 16 factions).
  Units and flags use the faction's dye color.
- **No overlapping territory:** a flag claims a circle of `FactionManager.CLAIM_RADIUS`
  (100) blocks across the ground at every height. Another faction's flag must be at least
  200 blocks away. A faction's own flags may overlap.
- **Vanilla stand-ins for art** until Warfist or Ted draw it: the flag is a vanilla banner in
  the faction's color (item and placed block), and the menu is drawn with flat fills and outlines, no textures. Nothing of Mojang's is copied into the mod. **Warfist (25 October 2026) says all of this art is final, not a stand-in:** the "null" block uses vanilla's missing-texture look on purpose (everyone knows it, and it fits the lore); the Crown of Chaos sprite is the one built already (it already combines all the crown ideas); the Faction Flag is a banner so players can make it their own in the vanilla loom; and the menu art is final apart from its palette, which now follows the Crown of Darkness, the mod's theme (see below).

**What part 1 does:**
- **Menu** (key J, "Faction Menu" under Controls → "Crown of Chaos - Faction System", the
  section for all faction keys, `FactionKeybind.CATEGORY`; crown ability keys stay under
  "Crown of Chaos"):
  - Without a faction: Create (name with emoji, up to 24 characters, plus a color) and Join
    (accept or decline invites, ask to join).
  - In a faction: Overview (owner can rename, recolor, toggle red alert, disband), Members
    (owner can invite by name, accept or decline requests, kick, make someone else owner),
    Relations (owner sets each other faction to Allowed, Neutral or Targeted), and Factions
    (a list of all of them).
  - The owner can't leave without making someone else owner or disbanding.
- **Name tags:** each faction is a scoreboard team named `coc_faction_<id>`, whose prefix is
  `[Name] ` in the faction's dye color, shown in chat and above heads. Player names stay
  white, because team colors are only the 16 chat colors.
- **Faction Flag item:**
  - Crafted from 8 wool (any color) around a Faction Coin. It belongs to the crafter's faction
    (data components `crownofchaos:flag_faction` and `crownofchaos:flag_color`; the item
    model picks the banner color from `flag_color`).
  - Placing it puts down a vanilla banner of the faction's color and records a claim. Only
    members can place it.
  - Each flag has a floating name label (a vanilla text display, `faction/FlagLabels`),
    kept in step every 5 seconds and on rename or recolor, and removed with the flag.
  - Only members can break a flag or the block holding it up; creative mode can too
    (`CREATIVE_BREAKS_ANY_FLAG`). Explosions skip flags (`mixin/ServerExplosionMixin`) and
    pistons can't move them or their support (`mixin/PistonStructureResolverMixin`).
    Breaking your own flag removes the claim and drops a Faction Flag of the same faction.
    Every 5 seconds the server drops claims whose banner is gone (commands, other mods).
- **Territory:** entering a faction's territory shows a title (the faction's name in its
  color, with "Faction Territory" or "Your Faction's Territory" underneath); leaving shows an
  action-bar note.
- **Red alert and stances** decide when units fight in their territory (see part 3).

**Code:**
- `faction/`:
  - `Faction`: the data and its codec. Players' copies omit flag positions.
  - `FactionData`: SavedData, saved as `data/crownofchaos/factions.dat`, with territory
    lookups.
  - `FactionManager`: all server logic. It handles menu actions with validation and chat
    replies, keeps teams in step, syncs to clients, shows territory titles and tracks flags.
  - `FactionStance` and `FactionFlag`.
- `network/FactionActionPayload`: one message per menu action; the javadoc lists each
  action's parameters. `network/FactionSyncPayload`: every faction, sent to every player
  whenever anything changes.
- `item/FactionFlagItem` and `item/ModComponents`.
- Client: `client/faction/FactionScreen`, the menu, with `FactionButton` and `ColorSwatch`
  (written by a helper agent), `FactionClientState` for the last synced factions, and
  `client/FactionKeybind`.
- Units should call `FactionManager.territoryAt(level, x, z)` and read
  `Faction.redAlert()` and `stanceToward(...)` to decide whom to attack.

**Tested in game** with a temporary debug hook and screenshots: creating a faction with an
emoji name; every menu tab; the chat tag; placing and breaking a flag; the territory title;
disbanding.
**Tested online by Warfist and his brother (25 October 2026), all went well:** invites, join requests, kick, handing over ownership, the 200-block spacing between factions, and seeing another faction's territory title.
**Also confirmed by Warfist on 25 October 2026:** pressing J to open the menu, crafting a flag, the unit command menu (number keys 1-7, V, free camera and movement), every unit recipe at the Commander's Table, the Targeted stance on its own, changing a faction's color while its units are out, units after reloading the world, and Hoglin units fighting what the faction rules pick.
**Not tested yet:** war between two real factions with units fighting each other, and a full Chaos King fight with several players, which Warfist wants to play again with the newest changes.
- Pressing J to open the menu, and crafting a flag.

**Known gaps:** a flag removed some other way (commands, other mods) drops a plain banner
instead of a Faction Flag (the claim is still removed). Placed banners and flag items keep
their old color if the faction changes color (labels and claims update).

**Part 5, war** (done 5 October 2026): owners declare war in the faction menu's War tab
(written by a helper agent) on whole factions (`Faction.warFactions`) or single players
(`warPlayers`, resolved by online name or any faction's member list), and make peace there.
Actions `DECLARE_WAR`, `END_WAR`, `DECLARE_WAR_ON_PLAYER`, `END_WAR_ON_PLAYER`. War is
one-sided (the target is told), makes units attack the target anywhere (`UnitManager.atWar`:
the target's faction, or the player or a unit's owner; it overrides Allowed), and blocks
Reclaim for the declaring faction's members. Disbanding a faction removes it from others'
wars.

**Next:** Warfist's faction plan is fully built (beta). Implement only what he asks
for next; reread `Crown Ideas.md` first, it changes often.

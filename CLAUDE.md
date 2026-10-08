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
finished crown.
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

Open ideas from `developing plans/Crown Ideas.md` (Warfist's planning file; it changes
often, so reread it, and implement only what Warfist asks for):

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
17 units for 8 crowns, from Warfist's plan plus Enderman and Shulker for the Crown of Void,
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
- Each player can have at most `UnitManager.MAX_UNITS_PER_PLAYER` (40, set by Warfist) units; `UnitItem` refuses
  to summon past it (counted over loaded chunks by `UnitManager.countUnits`).
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
  the faction's color (item and placed block), and the menu is drawn with flat fills and gold
  outlines, no textures. Nothing of Mojang's is copied into the mod.

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
**Not tested yet:**
- Anything that needs two players: invites, join requests, kick, handing over ownership,
  the 200-block spacing between factions, and seeing another faction's territory title.
  Test these over LAN with a second account.
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

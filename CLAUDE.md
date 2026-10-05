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

- **New crowns:** create them with `ModItems.registerCrown(...)` (adds the enchanted glint,
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
- **3D models:** convert Blockbench "Modded Entity" exports (Mojang mappings) from
  `3dmodels/` into `client/model/`, nesting the crown under a `head` part. Player-head
  template exports sit 24 pixels too low and include template head cubes to remove. Register
  the model for both the crown and its netherite version.

## Current state (October 2026)

Done: the Golden Crown plus nine magical crowns (Under, Poseidon, Nether, Darkness, Sun,
Wither, Winter, Iron, Void), each with a netherite version, description, recipe and
abilities (the Under Crown's secondary is Ore Sense, the Crown of Winter's is Icicle
Drop); materials (Sculk Cord from Wardens, Ankh Charm in desert pyramids); a Dragon Egg
on every dragon kill; the Crown of Chaos creative tab. Custom 3D models exist for the Under
Crown, Poseidon, Nether and Wither crowns; the others render as helmets.

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
- The rest of the **faction system** (see the next section): units, the Commander's Table,
  sentry, follow and attack commands, declaring war, reclaiming units.
- Remaining 3D models.

## Faction system (in progress)

Warfist's full plan is under "factions system" in `developing plans/Crown Ideas.md`. It's
being built in parts, and is labeled **alpha** in the README (keep that label until Warfist
says otherwise). **Part 1 (factions, menu, flags and territory) is done and committed** (4
October 2026).

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
- **Menu** (key J, "Faction Menu" under Controls → Crown of Chaos):
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
  - Crafted shapeless from any banner and a gold block. It belongs to the crafter's faction
    (data components `crownofchaos:flag_faction` and `crownofchaos:flag_color`; the item
    model picks the banner color from `flag_color`).
  - Placing it puts down a vanilla banner of the faction's color and records a claim. Only
    members can place it.
  - Anyone can break a flag. That removes the claim and drops a Faction Flag of the same
    faction. Every 5 seconds the server drops claims whose banner is gone (explosions and
    pistons).
- **Territory:** entering a faction's territory shows a title (the faction's name in its
  color, with "Faction Territory" or "Your Faction's Territory" underneath); leaving shows an
  action-bar note.
- **Red alert and stances** are saved and shown, but nothing acts on them yet. Units will.

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

**Known gaps:** a wall-mounted flag whose wall block is broken, or a flag destroyed by an
explosion, drops a plain banner instead of a Faction Flag (the claim is still removed).

**Next parts** (implement only what Warfist asks for, in his order): units (craftable unit
items per crown, friendly to the faction, colored by faction), the Commander's Table,
sentry mode, unit follow, unit attack, declaring war, and reclaiming units. Then make the
red alert and stance settings drive what units attack. Before starting, read the full plan
in `Crown Ideas.md` again; it changes often.

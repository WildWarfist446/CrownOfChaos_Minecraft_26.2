# Developing The Crown of Chaos

How to build, run and extend the mod. For what the mod does in game, see the
[README](README.md). Working with Claude? [CLAUDE.md](CLAUDE.md) has the context and
standing rules for AI sessions.

## Setup

Requires Java 25. The project uses Fabric Loom with Minecraft 26.2's official (Mojang)
names.

| Command | What it does |
|---|---|
| `./gradlew runClient` | Starts Minecraft with the mod loaded |
| `./gradlew build` | Builds the mod jar into `build/libs/` |
| `./gradlew vscode` | Generates VS Code launch configurations |
| `./gradlew genSources` | Decompiles Minecraft so you can read its source in your editor |

Test worlds and settings from `runClient` live in `run/`, which isn't committed.

## Project layout

| Path | Contents |
|---|---|
| `src/main/java/com/crownofchaos/item/ModItems.java` | Items (crowns, netherite crowns, materials), the creative tab, and loot (Sculk Cord, Ankh Charm) |
| `src/main/java/com/crownofchaos/item/FactionFlagItem.java`, `ModComponents.java` | The Faction Flag and the item data components that record its faction |
| `src/main/java/com/crownofchaos/item/CrownEffects.java` | Each crown's potion effects, passive bonuses (Darkness on hit, Fire Aspect, Flame, Wither on hit, freeze on hit, Swiftness on sand, snow, ice and End blocks, Crown of Void teleport), the pet check, and tuning values |
| `src/main/java/com/crownofchaos/faction/` | The faction system (alpha): faction data saved with the world (`FactionData`), and the server logic for menu actions, name tags, syncing, territory and flags (`FactionManager`) |
| `src/main/java/com/crownofchaos/entity/` | Entities the mod adds (`ModEntities`): the Crown of Winter's icicle |
| `src/main/java/com/crownofchaos/ability/` | Crown abilities, the registry that links each crown to its abilities, and shared ability helpers |
| `src/main/java/com/crownofchaos/network/` | Messages between the client and server (ability keys, Riptide launch, Ore Sense, faction menu actions and faction updates) |
| `src/main/java/com/crownofchaos/mixin/` | Changes that hook into Minecraft (Fortune, damage bonuses, a Dragon Egg on every dragon kill, volley arrows and shield blocking, crown fireball and wither skull explosions, Wither immunity, ice grip, walking on powder snow) |
| `src/client/java/com/crownofchaos/client/` | Client-only code: the ability keybinds and client-side ability effects |
| `src/client/java/com/crownofchaos/client/model/` | Crown 3D models and the renderer that draws them on the head |
| `src/client/java/com/crownofchaos/client/faction/` | The faction menu and the client's copy of the factions |
| `src/client/java/com/crownofchaos/client/render/` | Renderers for the mod's entities (the icicle) |
| `src/main/resources/assets/crownofchaos/` | Textures, item models, names and descriptions |
| `src/main/resources/data/crownofchaos/` | Crafting and smithing recipes, recipe-book unlocks, and block tags |
| `Sprites/` | Source artwork (Aseprite files and PNG exports) |
| `3dmodels/` | Blockbench 3D model exports for the crowns |

## Adding a crown

1. Register it in `ModItems` with `registerCrown(...)` (gives it the enchanted glint and its
   description line), plus `registerNetheriteCrown(...)` for its netherite version.
2. Add both to the creative tab list in `ModItems.initialize()`.
3. Add its effects to `CrownEffects` and its abilities to `CrownAbilities`.
4. Add the sprite to `assets/crownofchaos/textures/item/`, item model files for both
   versions (the netherite one points at the crown's model), and names and description in
   `lang/en_us.json`.
5. Add its crafting recipe and a netherite smithing recipe in `data/crownofchaos/recipe/`,
   each with a recipe-book unlock in `data/crownofchaos/advancement/recipes/combat/`.

Effects and abilities should check the worn crown with `CrownEffects.isWearing(...)` (or
`ModItems.baseCrown(...)`), so the netherite version gets the same powers automatically.

## Adding a crown ability

1. Create a class in `ability/` that implements `CrownAbility`. `activate` runs on the
   server when the key is pressed; return `true` to start the cooldown.
2. Attach it to a crown in `CrownAbilities.initialize()`:
   `register(ModItems.YOUR_CROWN, new YourAbility());` for the primary key (R), or
   `registerSecondary(ModItems.YOUR_CROWN, new YourAbility());` for the secondary key (G).
3. Add any action-bar messages to `assets/crownofchaos/lang/en_us.json`, styled with
   `CrownAbilities.MESSAGE_COLOR` so they match the other crown messages.

Shared helpers for common kinds of ability:

| Helper | Use it for |
|---|---|
| `ToggleState` | On/off abilities (Vein Miner, Frost Walker, Glowing Arrows): handles the state, the green/red messages and resetting when the player leaves |
| `ChargeTracker` | Abilities with several charges that recharge one at a time (Wind Charge, Wither Skull) |
| `FlightAbility` | Timed flight at a mob's speed with its particles and a safe landing (Blaze Flight, Wither Flight); add a factory method like `blazeFlight` |
| `AbilityTargeting.findLookTarget` | Abilities aimed at whatever the player is looking at (Sonic Boom, Arrow Volley, Lightning Strike) |
| `CrownAbilities.CROWN_ABILITY_TAG` | Add this tag to every projectile, cloud or other entity an ability creates; damage from tagged entities never hurts the wearer's own pets |
| `CrownAbilities.isHoldingAbilityKey` | Abilities that work while a key is held down |

## Adding a 3D model

1. In Blockbench, use a **Modded Entity** project and export with **Mojang mappings**. Put
   the `.java` file and its texture `.png` in `3dmodels/`.
2. Convert it into a model class in `client/model/` (see `UnderCrownModel`), with the crown
   parts nested under a part named `head` so they follow the wearer's head. Exports built on
   a player-head template sit 24 pixels too low and include the template's own head cubes;
   shift the crown up and leave those cubes out.
3. Copy the texture to `assets/crownofchaos/textures/entity/`.
4. Register it in `CrownModels.initialize()` for both the crown and its netherite version.

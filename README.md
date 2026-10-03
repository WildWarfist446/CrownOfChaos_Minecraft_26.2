# The Crown of Chaos

A Fabric mod for Minecraft 26.2 by Warfist that adds powerful crowns. Each crown is worn
in the helmet slot, protects like a diamond helmet, and grants its own set of abilities
for as long as it is worn.

## Crowns

All crowns have the same armor, toughness, durability and enchantability as a diamond
helmet and are repaired with diamonds. Their effects are applied while the crown is on
your head and removed as soon as it comes off; potion and beacon effects you already have
are left untouched.

### Under Crown

| Ability | Details |
|---|---|
| Haste II | Permanent while worn |
| Night Vision | Permanent while worn |
| Hero of the Village I | Permanent while worn |
| Fortune III | Applies to block drops such as ores and crops. If your tool has higher Fortune, the tool's level is used. |
| **Ability: Vein Miner** | Press the crown ability key (default **R**) to toggle Vein Miner on or off. While it's on, mining an ore breaks the whole connected vein, up to 64 extra blocks. Ores only. Each block costs normal tool durability, and mining stops before your tool would break. |

**Recipe** (unlocked by picking up a Golden Helmet):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Diamond Block | Emerald Block | Diamond Block |
| **Middle** | Lapis Lazuli Block | Golden Helmet | Lapis Lazuli Block |
| **Bottom** | Obsidian | Lantern | Obsidian |

### Crown of Poseidon

| Ability | Details |
|---|---|
| Water Breathing | Permanent while worn |
| Dolphin's Grace | Permanent while worn |
| Night Vision | Permanent while worn |
| Water damage bonus | Attacks deal 50% more damage while you are in water, including arrows and tridents you fire |
| **Ability: Riptide** | Press the crown ability key (default **R**) to launch yourself exactly like a Riptide III trident. Works in water or rain, not while riding. 4.5 second cooldown. |

**Recipe** (unlocked by picking up a Heart of the Sea):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Nautilus Shell | Heart of the Sea | Nautilus Shell |
| **Middle** | Prismarine Crystals | Golden Helmet | Prismarine Crystals |
| **Bottom** | Prismarine Shard | Trident | Prismarine Shard |

## Crown abilities

Every crown has an active ability, triggered with the **Use Crown Ability** key (default
**R**, rebindable under Options → Controls → Key Binds → Crown of Chaos) while the crown
is worn.

| Crown | Ability | Type | Cooldown |
|---|---|---|---|
| Under Crown | Vein Miner | Toggle: press to switch on or off | None |
| Crown of Poseidon | Riptide | Press to launch | 4.5 seconds |

- Abilities with a cooldown show it on the crown's icon, and pressing the key early shows
  the time remaining in the action bar.
- Toggles show "enabled" or "disabled" in the action bar, and reset to off when you leave
  the world or server.
- Pressing the key without a crown on shows "Wear a crown to use its power".

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2 and put it in your
   `mods` folder.
3. Put the Crown of Chaos jar in your `mods` folder.

The mod must be installed on both the client and the server.

## Development

Requires Java 25.

| Command | What it does |
|---|---|
| `./gradlew runClient` | Starts Minecraft with the mod loaded |
| `./gradlew build` | Builds the mod jar into `build/libs/` |
| `./gradlew vscode` | Generates VS Code launch configurations |

In a Creative world with cheats on, crowns can also be found in the Combat tab or given with:

```
/give @s crownofchaos:under_crown
/give @s crownofchaos:crown_of_poseidon
```

### Project layout

| Path | Contents |
|---|---|
| `src/main/java/com/crownofchaos/item/ModItems.java` | Item registration |
| `src/main/java/com/crownofchaos/item/CrownEffects.java` | Each crown's effects and tuning values |
| `src/main/java/com/crownofchaos/ability/` | Crown abilities and the registry that links each crown to its ability |
| `src/main/java/com/crownofchaos/network/` | Messages between the client and server (ability key, Riptide launch) |
| `src/main/java/com/crownofchaos/mixin/` | Passive bonuses that hook into Minecraft (Fortune, water damage) |
| `src/client/java/com/crownofchaos/client/` | Client-only code: the ability keybind and client-side ability effects |
| `src/main/resources/assets/crownofchaos/` | Textures, item models and names |
| `src/main/resources/data/crownofchaos/` | Crafting recipes |
| `Sprites/` | Source artwork (Aseprite files and PNG exports) |

### Adding a crown ability

1. Create a class in `ability/` that implements `CrownAbility`. `activate` runs on the
   server when the key is pressed; return `true` to start the cooldown.
2. Attach it to a crown in `CrownAbilities.initialize()`:
   `register(ModItems.YOUR_CROWN, new YourAbility());`
3. Add any action-bar messages to `assets/crownofchaos/lang/en_us.json`.

The server also tracks whether the key is held (`CrownAbilities.isHoldingAbilityKey`) for
abilities that should work while the key is held down.

## Planned

- Custom 3D crown models, made in Blockbench as Modded Entity exports (Mojang mappings).
  Until then, crowns look like a diamond helmet when worn.

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

**Recipe** (unlocked by picking up a Heart of the Sea):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Nautilus Shell | Heart of the Sea | Nautilus Shell |
| **Middle** | Prismarine Crystals | Golden Helmet | Prismarine Crystals |
| **Bottom** | Prismarine Shard | Trident | Prismarine Shard |

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
| `src/main/java/com/crownofchaos/mixin/` | Abilities that hook into Minecraft (Fortune, water damage) |
| `src/main/resources/assets/crownofchaos/` | Textures, item models and names |
| `src/main/resources/data/crownofchaos/` | Crafting recipes |
| `Sprites/` | Source artwork (Aseprite files and PNG exports) |

# The Crown of Chaos

A Fabric mod for Minecraft 26.2 by Warfist that adds powerful crowns. Craft a Golden
Crown, then upgrade it into one of four magical crowns. Each is worn in the helmet slot,
protects like a diamond helmet, and grants its own effects and active ability for as long
as it is worn.

## Crowns

All crowns except the Golden Crown have the same armor, toughness, durability and
enchantability as a diamond helmet and are repaired with diamonds. Their effects are applied while the crown is on
your head and removed as soon as it comes off; potion and beacon effects you already have
are left untouched.

### Golden Crown

*"Fit for a King."*

The base crown that every other crown is crafted from. It has the same stats as a golden
helmet, has no effects or ability (the crown ability key does nothing while it's worn),
and keeps Piglins calm like other gold armor.

**Recipe** (unlocked by picking up a Gold Ingot):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Gold Ingot | Emerald | Gold Ingot |
| **Middle** | Gold Ingot | Gold Ingot | Gold Ingot |

### Under Crown

*"Diggy, Diggy Hole"*

| Ability | Details |
|---|---|
| Haste II | Permanent while worn |
| Night Vision | Permanent while worn |
| Hero of the Village I | Permanent while worn |
| Fortune III | Applies to block drops such as ores and crops. If your tool has higher Fortune, the tool's level is used. |
| **Ability: Vein Miner** | Press the crown ability key (default **R**) to toggle Vein Miner on or off. While it's on, mining an ore breaks the whole connected vein, up to 64 extra blocks. Ores only. Each block costs normal tool durability, and mining stops before your tool would break. |

**Recipe** (unlocked by picking up a Golden Crown):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Diamond Block | Emerald Block | Diamond Block |
| **Middle** | Lapis Lazuli Block | Golden Crown | Lapis Lazuli Block |
| **Bottom** | Obsidian | Lantern | Obsidian |

### Crown of Poseidon

*"A shame. I quite like blowfish. I am Poseidon."*

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
| **Middle** | Prismarine Crystals | Golden Crown | Prismarine Crystals |
| **Bottom** | Prismarine Shard | Trident | Prismarine Shard |

### Crown of Nether

*"The Fires of Avernus can't get you here.."*

| Ability | Details |
|---|---|
| Fire Resistance | Permanent while worn: immune to fire and lava damage |
| **Ability: Fire Charge** | Press the crown ability key (default **R**) to shoot a Ghast fireball where you're looking, no fire charge item needed. It flies 25% faster than a Ghast's and explodes like one, hurting nearby mobs, but never breaks blocks or starts fires. The blast can hurt you too, so don't fire it at point-blank range. 2 second cooldown. |

**Recipe** (unlocked by picking up a Nether Star):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Gold Block | Nether Star | Gold Block |
| **Middle** | Blaze Rod | Golden Crown | Blaze Rod |
| **Bottom** | Ghast Tear | Netherrack | Ghast Tear |

### Crown of Darkness

*"Screeches echo in the Darkness..."*

| Ability | Details |
|---|---|
| Night Vision | Permanent while worn |
| Darkness on hit | Anything you hit, including with arrows and tridents you fire, gets the Warden's Darkness effect for 20 seconds |
| **Ability: Sonic Boom** | Look at a mob or player within 15 blocks and press the crown ability key (default **R**). Like the Warden, it charges for 1.7 seconds, then blasts that target for 10 damage that ignores armor, with heavy knockback, even through walls. 15 second cooldown. |

**Recipe** (unlocked by picking up a Sculk Cord):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Echo Shard | Sculk Cord | Echo Shard |
| **Middle** | Echo Shard | Golden Crown | Echo Shard |
| **Bottom** | Diamond Block | Sculk Shrieker | Diamond Block |

## Materials

| Item | Source | Use |
|---|---|---|
| Sculk Cord | Every Warden drops one when killed, alongside its Sculk Catalyst | Crafting the Crown of Darkness |

The Sculk Cord's description reads *"The screeching has finally ceased..."*

## Crown abilities

Every crown except the Golden Crown has an active ability, triggered with the **Use Crown Ability** key (default
**R**, rebindable under Options → Controls → Key Binds → Crown of Chaos) while the crown
is worn.

| Crown | Ability | Type | Cooldown |
|---|---|---|---|
| Under Crown | Vein Miner | Toggle: press to switch on or off | None |
| Crown of Poseidon | Riptide | Press to launch | 4.5 seconds |
| Crown of Nether | Fire Charge | Press to shoot | 2 seconds |
| Crown of Darkness | Sonic Boom | Press while looking at a target | 15 seconds |

- Abilities with a cooldown show it on the crown's icon, and pressing the key early shows
  the time remaining in the action bar.
- Toggles show "enabled" or "disabled" in the action bar, and reset to off when you leave
  the world or server.
- Pressing the key without a crown on shows "Wear a crown to use its power".
- Ability messages are shown in light purple to match the crowns' item names, except Vein
  Miner's toggle messages, which are green (enabled) and red (disabled).

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
/give @s crownofchaos:golden_crown
/give @s crownofchaos:under_crown
/give @s crownofchaos:crown_of_poseidon
/give @s crownofchaos:crown_of_nether
/give @s crownofchaos:crown_of_darkness
/give @s crownofchaos:sculk_cord
```

### Project layout

| Path | Contents |
|---|---|
| `src/main/java/com/crownofchaos/item/ModItems.java` | Item registration |
| `src/main/java/com/crownofchaos/item/CrownEffects.java` | Each crown's effects and tuning values |
| `src/main/java/com/crownofchaos/ability/` | Crown abilities and the registry that links each crown to its ability |
| `src/main/java/com/crownofchaos/network/` | Messages between the client and server (ability key, Riptide launch) |
| `src/main/java/com/crownofchaos/mixin/` | Changes that hook into Minecraft (Fortune, water damage, crown fireball explosions) |
| `src/client/java/com/crownofchaos/client/` | Client-only code: the ability keybind and client-side ability effects |
| `src/client/java/com/crownofchaos/client/model/` | Crown 3D models and the renderer that draws them on the head |
| `src/main/resources/assets/crownofchaos/` | Textures, item models and names |
| `src/main/resources/data/crownofchaos/` | Crafting recipes |
| `Sprites/` | Source artwork (Aseprite files and PNG exports) |
| `3dmodels/` | Blockbench 3D model exports for the crowns |

### Adding a crown ability

1. Create a class in `ability/` that implements `CrownAbility`. `activate` runs on the
   server when the key is pressed; return `true` to start the cooldown.
2. Attach it to a crown in `CrownAbilities.initialize()`:
   `register(ModItems.YOUR_CROWN, new YourAbility());`
3. Add any action-bar messages to `assets/crownofchaos/lang/en_us.json`, styled with
   `CrownAbilities.MESSAGE_COLOR` so they match the other crown messages.

The server also tracks whether the key is held (`CrownAbilities.isHoldingAbilityKey`) for
abilities that should work while the key is held down.

## 3D models

Crowns can have a custom 3D model when worn, made in Blockbench as a Modded Entity export
(Mojang mappings) and kept in `3dmodels/`.

| Crown | Worn model |
|---|---|
| Golden Crown | Golden helmet (3D model planned) |
| Under Crown | Custom 3D model |
| Crown of Poseidon | Custom 3D model |
| Crown of Nether | Diamond helmet (3D model planned) |
| Crown of Darkness | Diamond helmet (3D model planned) |

### Adding a 3D model

1. Export the model from Blockbench and put the `.java` file and its texture `.png` in
   `3dmodels/`.
2. Convert it into a model class in `client/model/` (see `UnderCrownModel`), with the crown
   parts nested under a part named `head` so they follow the wearer's head.
3. Copy the texture to `assets/crownofchaos/textures/entity/`.
4. Register it in `CrownModels.initialize()`.

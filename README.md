# The Crown of Chaos

In the vast and ever-changing world of Minecraft, whispers of a new and unimaginable power have begun to spread. Legends speak of the ancient Crown of Chaos, a dark artifact imbued with unimaginable strength. To craft this crown, however, a hero must first face the fiercest challenges the land has ever known.

A Fabric mod for Minecraft 26.2 by Warfist that adds powerful crowns. Craft a Golden
Crown, then upgrade it into one of the magical crowns. Each is worn in the helmet slot,
protects like a diamond helmet, and grants its own effects and abilities for as long as it
is worn. Every magical crown can also be upgraded to netherite.

> **AI disclosure:** The code for this mod was written with the help of AI (Claude, by
> Anthropic). All of the art (item sprites, the logo and the 3D crown models) was drawn by
> hand by the mod's creators.

**Contents:** [Crowns](#crowns) · [Netherite crowns](#netherite-crowns) ·
[Abilities](#abilities) · [Materials](#materials) · [Factions (beta)](#factions-beta) ·
[Changes to vanilla](#changes-to-vanilla) ·
[Installation](#installation) · [Finding the items](#finding-the-items) ·
[3D models](#3d-models) · [Development](#development) · [Credits](#credits)

## Crowns

Every crown shimmers with the enchanted glint, in the inventory and when worn. All crowns
except the Golden Crown have the same armor, toughness, durability and enchantability as a
diamond helmet and are repaired with diamonds. Their effects are applied while the crown is
on your head and removed as soon as it comes off; potion and beacon effects you already
have are left untouched.

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
| **Secondary ability: Ore Sense** | Press the secondary crown ability key (default **G**) to see every ore within 16 blocks through walls for 40 seconds, outlined in its ore's color: gray coal, tan iron, orange copper, yellow gold, red redstone, blue lapis, cyan diamond, green emerald, white quartz and brown ancient debris (ores from other mods are white). The action bar says how many were found. Only you see the outlines, and a mined ore's outline disappears. 60 second cooldown. |

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
| **Secondary ability: Lightning Strike** | Look at a mob or player within 30 blocks and press the secondary crown ability key (default **G**) to strike it with lightning. Slightly weaker than natural lightning: 4 damage and 4 seconds of fire to everything within 2 blocks (vanilla is 5 damage, 8 seconds, 3 blocks), and it never sets blocks on fire or transforms mobs (no charged creepers or witches). Never hurts you or your pets. 20 second cooldown. |

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
| Fire Aspect | Your melee hits set the target on fire for 8 seconds, like Fire Aspect II |
| Flame | Arrows you fire are lit like Flame arrows, setting targets on fire and lighting TNT |
| **Ability: Fire Charge** | Press the crown ability key (default **R**) to shoot a Ghast fireball where you're looking, no fire charge item needed. It flies 25% faster than a Ghast's and explodes like one but deals 50% more damage (9 on a direct hit), and sets everything it damages on fire for 5 seconds. It never breaks blocks or starts fires on the ground. The blast can hurt you too, so don't fire it at point-blank range. 2 second cooldown. |
| **Secondary ability: Blaze Flight** | Press the secondary crown ability key (default **G**) to fly for 60 seconds, slowly like a hovering Blaze. It works like creative flight (double-tap jump; jump and sneak to rise and sink) at 40% of creative speed, smoking and crackling like a Blaze the whole time, with a countdown in the action bar for the last 10 seconds. When it ends you get Slow Falling until you land (up to 30 seconds), so you can't take fall damage. Ends early if you take the crown off. 2 minute cooldown, counted from takeoff. |

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
| **Secondary ability: Darkness Pulse** | Press the secondary crown ability key (default **G**) to let out a shriek that gives every other player within 30 blocks the Warden's Darkness for 20 seconds. Like the Warden's, it only affects players. 30 second cooldown. |

**Recipe** (unlocked by picking up a Sculk Cord):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Echo Shard | Sculk Cord | Echo Shard |
| **Middle** | Echo Shard | Golden Crown | Echo Shard |
| **Bottom** | Diamond Block | Sculk Shrieker | Diamond Block |

### Crown of Sun

*"The supreme sun god who traveled across the sky."*

| Ability | Details |
|---|---|
| Swiftness II | While standing on sand, red sand, or any sandstone block, slab, stair or wall. Lasts 1 second after stepping off, so it doesn't drop out while jumping. |
| **Ability: Wind Charge** | Press the crown ability key (default **R**) to throw a wind charge, exactly like the item, no wind charge needed. The crown holds 3 charges; spent charges come back one at a time, 6 seconds each. The action bar shows charges left after every throw. |
| **Secondary ability: Sun Cycle** | Press the secondary crown ability key (default **G**) to switch between day and night, like `/time set day` or `/time set night`. Changes the time for everyone in that dimension. Only works where there's a day cycle (not the Nether or the End). 20 second cooldown. |

**Recipe** (unlocked by picking up an Ankh Charm):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Gold Block | Ankh Charm | Gold Block |
| **Middle** | Clock | Golden Crown | Cactus |
| **Bottom** | Sand | Diamond Block | Sand |

### Crown of Wither

*"I've seen this in a painting..."*

| Ability | Details |
|---|---|
| Wither immunity | Immune to the Wither effect from any source: Wither Skeletons, the Wither, wither roses and potions |
| Wither on hit | Your melee hits give the target Wither I for 10 seconds, like a Wither Skeleton's |
| **Ability: Wither Skull** | Press the crown ability key (default **R**) to shoot a wither skull where you're looking, like the Wither: 8 damage and Wither II on a hit (10 seconds on Normal, 40 on Hard, none on Easy), and you heal 2.5 hearts if it kills. The small explosion never breaks blocks. Holds 3 skulls, each taking 6 seconds to recharge, with 3 seconds between shots. |
| **Secondary ability: Wither Flight** | Press the secondary crown ability key (default **G**) to fly for 60 seconds at 40% of creative flight speed (the same as Blaze Flight), trailing smoke from your head like the Wither does. It works like creative flight (double-tap jump; jump and sneak to rise and sink), with a countdown in the action bar for the last 10 seconds. When it ends you get Slow Falling until you land (up to 30 seconds), so you can't take fall damage. Ends early if you take the crown off. 2 minute cooldown, counted from takeoff. |

**Recipe** (unlocked by picking up a Wither Skeleton Skull):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Wither Skeleton Skull | Nether Star | Wither Skeleton Skull |
| **Middle** | Soul Sand | Golden Crown | Soul Sand |
| **Bottom** | Bone | Diamond Block | Bone |

### Crown of Winter

*"Gunter, why did you gunt my fries?"*

| Ability | Details |
|---|---|
| Freeze on hit | Every hit you land, melee or with arrows and tridents you fire, freezes the target like powder snow: frosty and slowed, taking freeze damage, for about 7 seconds |
| Freeze immunity | You never freeze, in powder snow or from other wearers' hits |
| Snow and ice | Swiftness II on snow, snow blocks, powder snow and every kind of ice (lasts 1 second after stepping off), normal grip on ice instead of sliding, and you walk on top of powder snow |
| **Ability: Frost Walker** | Press the crown ability key (default **R**) to toggle Frost Walker II on or off. While it's on, still water under you freezes into frosted ice like Frost Walker II boots, and magma blocks and campfires don't burn you. |
| **Secondary ability: Icicle Drop** | Look at a mob or player within 30 blocks and press the secondary crown ability key (default **G**) to drop 5 icicles on it, 0.35 seconds apart. Each icicle appears 5 blocks above its head (lower under a ceiling), hangs for a moment and then falls fast. Every icicle that hits deals 4 freeze damage (2 hearts) and freezes the target for about 10 seconds: fully frozen for 7, then thawing. Blazes, Magma Cubes and Striders take 5 times the damage, as with any freezing. Icicles shatter when they land and never place blocks. Never hurts you or your pets. 6 second cooldown. |

**Recipe** (unlocked by picking up a Powder Snow Bucket):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Ice | Diamond Block | Ice |
| **Middle** | Gold Block | Golden Crown | Gold Block |
| **Bottom** | Snowball | Powder Snow Bucket | Snowball |

### Crown of Iron

*"HRMM!"*

| Ability | Details |
|---|---|
| Bow and crossbow damage | Arrows you fire from a bow or crossbow deal 50% more damage |
| **Ability: Arrow Volley** | Look at a mob or player within 20 blocks and press the crown ability key (default **R**). 24 arrows rain down from 3 blocks above its head over 1.5 seconds, ahead of a moving target: about half are aimed at its body and gently correct course, the rest scatter around it. A target that keeps moving can dodge some. Volley arrows deal 25% more damage than normal arrows, every one can land even right after another hit, and they can't be picked up. A raised shield blocks every volley arrow, whichever way the player faces. 10 second cooldown. |
| **Secondary ability: Glowing Arrows** | Press the secondary crown ability key (default **G**) to toggle Glowing Arrows on or off. While it's on, anything hit by an arrow you fire from a bow or crossbow glows for 2 minutes, so you can see it through walls. Only arrows from your bow or crossbow count (not Arrow Volley), and your own pets never glow. |

**Recipe** (unlocked by picking up a Totem of Undying):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Bow | Totem of Undying | Bow |
| **Middle** | Emerald | Golden Crown | Emerald |
| **Bottom** | Emerald | Diamond Block | Emerald |

### Crown of Void

*"Congratulations, you killed the dragon."*

| Ability | Details |
|---|---|
| Teleport when hit | Getting hit teleports you up to 5 blocks to a safe spot to your attacker's left or right (not straight back, so archers can't keep a clean shot) and turns you to face them. Hits with no attacker, like falls or lava, teleport you to a random safe spot nearby. At most once every 5 seconds, and not from ender pearl landing damage |
| Swiftness II | While standing on end stone, end stone bricks, purpur, obsidian or End portal frames (and their slabs, stairs and walls). Lasts 1 second after stepping off |
| **Ability: Ender Pearl** | Press the crown ability key (default **R**) to throw an ender pearl, exactly like the item, no pearl needed. 3 second cooldown. |
| **Secondary ability: Dragon's Breath** | Press the secondary crown ability key (default **G**) to surround yourself with the Ender Dragon's breath: a cloud that starts 3 blocks wide, spreads to 7 over 30 seconds, and deals 6 damage a second to everything inside except you and your pets, undead mobs included. It can't be bottled. 30 second cooldown. |

**Recipe** (unlocked by picking up the Dragon Egg):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | Purpur Block | Chorus Fruit | Purpur Block |
| **Middle** | Eye of Ender | Golden Crown | Eye of Ender |
| **Bottom** | End Stone | Dragon Egg | End Stone |

Every time the Ender Dragon is defeated, including after resummoning it with End Crystals, a
new Dragon Egg appears on the exit portal (vanilla only gives one, the first time). If an
egg from an earlier fight is still there, no extra one is added.

### Netherite crowns

Every crown except the Golden Crown can be upgraded at a smithing table, just like a diamond
helmet: a **Netherite Upgrade Smithing Template**, the crown, and a **Netherite Ingot**. The
result is "Netherite" plus the crown's name (for example, Netherite Crown of Sun). It keeps
the crown's enchantments, effects, abilities and look, and gets netherite helmet stats: more
armor and toughness, knockback resistance, more durability, and it doesn't burn in fire or
lava. The recipes unlock when you pick up a Netherite Ingot.

## Abilities

Each crown grants passive effects for as long as it's worn, plus an active ability on the
**Use Crown Ability** key (default **R**). Some crowns also have a secondary ability on the
**Use Secondary Crown Ability** key (default **G**). Both keys can be rebound under Options →
Controls → Key Binds → Crown of Chaos. Primary and secondary abilities have separate
cooldowns, so using one never blocks the other. See each crown's section above for the full
details.

| Crown | Passive effects | Ability (R) | Secondary ability (G) | Recipe unlocked by |
|---|---|---|---|---|
| Golden Crown | None (base crown) | None | None | Gold Ingot |
| Under Crown | Haste II, Night Vision, Hero of the Village, Fortune III | Vein Miner (toggle) | Ore Sense (60s) | Golden Crown |
| Crown of Poseidon | Water Breathing, Dolphin's Grace, Night Vision, +50% damage in water | Riptide (4.5s) | Lightning Strike, aimed (20s) | Heart of the Sea |
| Crown of Nether | Fire Resistance, Fire Aspect, Flame | Fire Charge (2s) | Blaze Flight, 60s of flight (2 min) | Nether Star |
| Crown of Darkness | Night Vision, Darkness on hit | Sonic Boom, aimed (15s) | Darkness Pulse (30s) | Sculk Cord |
| Crown of Sun | Swiftness II on sand | Wind Charge (3 charges, 6s each) | Sun Cycle (20s) | Ankh Charm |
| Crown of Wither | Wither immunity, Wither on hit | Wither Skull (3 charges, 6s each, 3s between shots) | Wither Flight, 60s of flight (2 min) | Wither Skeleton Skull |
| Crown of Winter | Freeze on hit, freeze immunity, Swiftness and grip on snow and ice | Frost Walker (toggle) | Icicle Drop, 5 icicles, aimed (6s) | Powder Snow Bucket |
| Crown of Iron | +50% bow and crossbow damage | Arrow Volley, aimed (10s) | Glowing Arrows (toggle) | Totem of Undying |
| Crown of Void | Teleport when hit, Swiftness on End blocks | Ender Pearl (3s) | Dragon's Breath (30s) | Dragon Egg |

Times in brackets are cooldowns. "Aimed" abilities hit whatever you're looking at.

- Primary abilities with a cooldown show it on the crown's icon, and pressing the key early
  shows the time remaining in the action bar. Secondary ability cooldowns show in the action
  bar only.
- Charge-based abilities (Wind Charge, Wither Skull) show the charges left after every use
  instead of a cooldown.
- Toggles show "enabled" or "disabled" in the action bar, and reset to off when you leave
  the world or server.
- Pressing a key without a crown on shows "Wear a crown to use its power", and pressing
  **G** on a crown without a secondary ability says so.
- Crown abilities never hurt animals you've tamed (wolves, cats, parrots, horses and so on),
  and crown on-hit effects (Darkness, Wither, freezing, Fire Aspect) don't apply to them.
  Other players' pets can still be hit.
- Ability messages are shown in light purple to match the crowns' item names, except the
  toggle messages (Vein Miner, Frost Walker, Glowing Arrows), which are green (enabled) and
  red (disabled).

## Materials

| Item | Source | Use |
|---|---|---|
| Sculk Cord | Every Warden drops one when killed, alongside its Sculk Catalyst | Crafting the Crown of Darkness |
| Ankh Charm | Desert pyramids: every pyramid has exactly one, in one of its four chests | Crafting the Crown of Sun |
| Faction Coin | Crafted from 8 Gold Ingots around 1 Emerald (unlocked by picking up an Emerald) | Crafting the Commander's Table and Faction Flags |

Descriptions: the Sculk Cord reads *"The screeching has finally ceased..."* and the Ankh
Charm reads *"Ra would be proud."*

## Factions (beta)

> **Beta:** the faction system's main features are complete: factions, the faction menu,
> territory, the Commander's Table, army units you can summon and command, and war. It's
> still being tested and balanced, so details may change between versions.

Players can band together into factions, each with its own name and color, and claim land
with faction flags.

**Faction menu:** press **J** (rebindable under Options → Controls → Key Binds → Crown of
Chaos - Faction System).

- **Without a faction:**
  - **Create:** found a faction. Pick a name of up to 24 characters (emoji allowed, unique)
    and one of the 16 dye colors. No two factions can share a color.
  - **Join:** accept or decline invitations, or ask to join any faction.
- **In a faction:**
  - **Overview:** your faction's details. The owner can rename it, change its color, turn red
    alert on or off, and disband it.
  - **Members:** the member list. The owner can invite online players by name, accept or
    decline join requests, remove members and hand over ownership. The owner can't leave
    without handing over ownership or disbanding first.
  - **Relations:** the owner sets each other faction to **Allowed** (may enter your territory
    unharmed), **Neutral** or **Targeted** (attacked in your territory).
  - **War:** the owner declares war on factions or players, or makes peace (see **War** below).
  - **Factions:** every faction, with its owner, members and flags.

**Name tags:** members' names show their faction as a colored `[Faction Name]` tag in chat
and above their heads.

**Faction Flag:**
- Crafted from 8 wool of any color around a Faction Coin (unlocked by picking up a Faction
  Coin). It belongs to the faction of whoever crafts it, and shows as a banner in that
  faction's color.
- Placing it claims everything within **100 blocks** of it, at any height. Only members can
  place their faction's flags.
- Flags of different factions must be at least 200 blocks apart, so territories never
  overlap. Your own faction's flags can overlap to grow your land.
- The faction's name floats above each of its flags, in its color.
- Flags are protected: only the faction's members can break a flag or the block it stands on
  or hangs from (players in creative mode can too). Explosions and pistons can't destroy
  them either. Breaking your own flag removes the claim, and the flag drops to place again.

**Territory:** entering a faction's territory shows its name in large letters in its color,
with "Faction Territory" (or "Your Faction's Territory") underneath. Leaving shows a short
note above the hotbar.

**Red alert and relations** decide when your army units fight in your territory: they
attack Targeted factions there, and on red alert everyone who isn't Allowed (see Summoning
units below).

### Commander's Table and units

The Commander's Table makes army units. Put a crown in its crown slot to see the units that
crown commands, pick one, and put the unit's two cost items in the cost slots (in either
order). The unit item appears on the right; taking it uses up the cost items, and the crown
stays. Shift-click makes as many as the cost items allow. Netherite crowns command the same
units as their crown. Like a crafting table, it keeps no items: closing it gives them back.
It turns to face you when placed.

**Recipe** (unlocked by picking up a Faction Coin):

| | Left | Middle | Right |
|---|---|---|---|
| **Top** | | Empty Map | |
| **Middle** | Diamond | Faction Coin | Diamond |
| **Bottom** | Planks (any) | Planks (any) | Planks (any) |

| Crown | Unit | Cost |
|---|---|---|
| Crown of Wither | Wither Skeleton | 20 Nether Wart + 1 Iron Sword |
| Crown of Winter | Stray | 15 Snow Blocks + 1 Bow |
| | Polar Bear | 15 Wool (any color) + 3 Raw Cod |
| | Snow Golem | 4 Snow Blocks + 1 Jack o'Lantern |
| Crown of Iron | Pillager | 3 Emeralds + 1 Crossbow |
| | Vindicator | 5 Emeralds + 1 Iron Axe |
| | Ravager | 20 Emeralds + 1 Saddle |
| | Evoker | 15 Emeralds + 4 Blaze Powder |
| Crown of Darkness | Warden | 10 Diamonds + 1 Echo Shard |
| Crown of Sun | Husk | 5 Rotten Flesh + 2 Gold Ingots |
| | Parched | 10 Bones + 1 Bow |
| Crown of Poseidon | Guardian | 30 Kelp + 2 Prismarine Shards |
| Crown of Nether | Piglin | 5 Raw Porkchops + 3 Gold Ingots |
| | Hoglin | 10 Raw Porkchops + 5 Gold Ingots |
| | Ghast | 10 Blaze Powder + 1 Ghast Tear |
| Crown of Void | Enderman | 5 Ender Pearls + 5 End Stone |
| | Shulker | 5 Purpur Blocks + 2 Diamonds |

The Golden Crown and Under Crown command no units. Unit items look like the mob's spawn egg
and are named after it ("Pillager Unit").

**Summoning units:** use a unit item on a block, like a spawn egg, to summon the mob as a
unit of your faction (you must be in one). Units:

- are tinted in a bright, vivid version of your faction's color and outlined in it (up to 64 blocks away,
  only while you can see them), so anyone can tell whose they are at a glance; they change color
  with the faction (white factions are only outlined, as white leaves the mob unchanged);
- never despawn, don't burn in daylight, and Piglins and Hoglins don't turn into zombies
  outside the Nether;
- Warden units stay above ground (they never dig away) and don't give anyone Darkness;
- are **passive** toward players and other units: they don't attack them, and don't fight back, except
  - against **wild hostile mobs** (zombies, skeletons, creepers and the like) that are attacking
    you (their owner) or your faction's units, or, when told to **Attack**, any within 24 blocks;
  - inside your faction's territory, against players and units of factions you've set to
    **Targeted**, and while **red alert** is on, against everyone who isn't **Allowed**
    (players with no faction included);
  - anywhere, against factions and players you're at war with (see **War** below);
- never attack your own faction's members or units.

**Commanding units:** you can command your faction's units whose crown you're wearing (wear
the Crown of Iron to command Pillagers, Vindicators, Ravagers and Evokers).

- **Right-click** a unit to make it a sentry; right-click again to free it.
- **Sneak and right-click** a unit to select it, or deselect it. Selected units get a white outline, for you
  only.
- Orders go to your **selected units**, or, with none selected, to **all your units within
  32 blocks**.
- **Unit Commands** key (default **V**) toggles the command menu, a slim see-through row above
  your hotbar and status bars (so it never covers your items), with a small status line and, when you
  have nothing to command, a tiny "Sneak + Click to select one unit" hint above it, away from the crosshair. It doesn't take over the mouse, camera or movement, so you can keep
  fighting with it open. Each order has a number key (1-7), so
  V then 3 orders an attack without aiming. The menu stays open after an order, until you press V again:
  - **Follow Me:** they follow you, teleporting to you if they fall 24 blocks behind, so you
    can lead them into enemy territory. They still fight mobs that attack you or your units, and
    come back to you if they end up more than 40 blocks away.
  - **Sentry:** they stand guard where they are. Any hostile mob, or enemy by the faction rules,
    that comes into their aggro range makes them move to fight it, chasing at most 16 blocks from
    their post; once it's dead or gone they walk back to their post.
  - **Attack:** they hunt enemies within 24 blocks, anywhere: players and units of every
    faction you haven't set to Allowed. This overrides sentry and following.
  - **Free:** no orders; back to wandering, passive except for the territory rules above.
  - **Reclaim:** they turn back into their unit items, in your inventory (not while your faction
    is at war).
  - **Select:** selects the unit you're looking at; hold **Sneak** to select every unit of that
    type nearby instead. With no unit in your crosshair, it selects all your units nearby.
  - **Clear Selection**.
- **Units: Follow / Stop Following** key (default **B**) toggles following.
- If one of your units dies where you can't see it, you're told where.

Both keys can be rebound under Options → Controls → Key Binds → Crown of Chaos - Faction
System.

**War:** the faction owner declares war in the faction menu's **War** tab.

- **On a faction:** your units attack all its members and their units, anywhere, not just in
  your territory. War overrides the Allowed stance.
- **On a player:** type their name (online, or a member of any faction). Your units attack only
  them and the units they own, ignoring the rest of their faction. Works on players with no
  faction too.
- War is one-sided: the other side is told, and their units only fight back if they declare
  war too.
- **Make Peace** ends it, per faction or per player.
- While your faction is at war with anyone, its members can't reclaim units.

## Changes to vanilla

These apply to everyone in a world with the mod, not just crown wearers:

- Every Ender Dragon defeat places a Dragon Egg on the exit portal, including resummoned
  dragons (vanilla only does this the first time). No extra egg is added if one is already
  there.
- Wardens drop a Sculk Cord, alongside their Sculk Catalyst.
- Every desert pyramid has an Ankh Charm in one of its four chests.
- Explosions and pistons can't destroy faction flags or the blocks holding them up.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2 and put it in your
   `mods` folder.
3. Put the Crown of Chaos jar in your `mods` folder.

The mod must be installed on both the client and the server, and players can't join without it.
Minecraft 26.2 needs Java 25 (the server needs it too). On a server, use the Fabric API version built
for 26.2 (0.161.0 or newer).

## Finding the items

In Creative mode, every mod item is in the **Crown of Chaos** tab (crowns are also in the
Combat tab and materials in the Ingredients tab). With cheats on, they can also be given with:

```
/give @s crownofchaos:golden_crown
/give @s crownofchaos:under_crown
/give @s crownofchaos:crown_of_poseidon
/give @s crownofchaos:crown_of_nether
/give @s crownofchaos:crown_of_darkness
/give @s crownofchaos:crown_of_sun
/give @s crownofchaos:crown_of_wither
/give @s crownofchaos:crown_of_winter
/give @s crownofchaos:crown_of_iron
/give @s crownofchaos:crown_of_void
/give @s crownofchaos:netherite_crown_of_winter   (netherite_ + any crown's name)
/give @s crownofchaos:sculk_cord
/give @s crownofchaos:ankh_charm
/give @s crownofchaos:faction_flag   (belongs to whoever places it first)
/give @s crownofchaos:faction_coin
/give @s crownofchaos:commander_table
/give @s crownofchaos:pillager_unit   (any unit's mob name + _unit)
```

## 3D models

Some crowns have a custom 3D model when worn; the rest look like a helmet until theirs is
made.

| Crown | Worn model |
|---|---|
| Golden Crown | Golden helmet (3D model planned) |
| Under Crown | Custom 3D model |
| Crown of Poseidon | Custom 3D model |
| Crown of Nether | Custom 3D model |
| Crown of Darkness | Custom 3D model |
| Crown of Sun | Diamond helmet (3D model planned) |
| Crown of Wither | Custom 3D model |
| Crown of Winter | Diamond helmet (3D model planned) |
| Crown of Iron | Diamond helmet (3D model planned) |
| Crown of Void | Diamond helmet (3D model planned) |

The Commander's Table also has its own custom 3D model.

## Development

Building the mod, the project layout, and how to add crowns, abilities and 3D models are
covered in [DEVELOPMENT.md](DEVELOPMENT.md).

## Credits

Created by Warfist. All art, including the item sprites, logo and 3D crown models, was drawn
by hand by the mod's creators. The code was written with the help of AI (Claude, by
Anthropic).

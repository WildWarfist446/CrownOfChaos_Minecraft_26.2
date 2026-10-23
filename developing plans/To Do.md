# To Do

What's left for The Crown of Chaos, as of 6 October 2026. Tick items off with `[x]`.

## First thing next session

- [x] Unit command menu (V): don't lock the camera or movement while it's open, so players can
      keep fighting. Changing the held item only by scroll wheel while it's open is fine.
      (Today it's a normal menu screen, which takes over the mouse and keyboard; it likely needs
      to become an on-screen overlay that keeps the game in control, with the number keys still
      giving orders.)

## The Crown of Chaos (last crown in the plan)

- [x] Incomplete Crown of Chaos: crafted from all nine crowns; can't be worn or used
- [x] Soul of Chaos: the boss's item drop, a material used only for the Crown of Chaos
- [x] Chaos Arena dimension: a floating round arena in a dark purple void, glitched floor, Herobrine altar (lighting it is the fight trigger; for now it only announces itself)
- [x] Chaos Arena has no natural mob spawns and no treasure (only the boss and its drops)
- [x] Lava wall around the arena (never flows)
- [x] Glitched portal: throw Disc 11 into a nether portal; the frame becomes "null" blocks and the obsidian random blocks; breaking the null blocks destroys the whole portal
- [ ] "Null" block art (purple and black)
- [x] Herobrine, The Chaos King: a player-style boss in Warfist's skin with the Crown of Chaos; the altar spawns him in the fire with lightning, he floats up, four waves of units, then he lands and fights with melee and the Wither Skull, Fire Charge and Lightning Strike every 30 s; drops the Soul of Chaos; soul fire turns a Netherite Upgrade thrown into it into a Chaos Upgrade
- [ ] Tune the Chaos King fight in play (health, damage, wave sizes, his lines)
- [x] Chaos smithing template item ("Chaos Upgrade"); how to get it comes after the boss
- [x] Smithing recipe: Incomplete Crown of Chaos + Soul of Chaos + Chaos Upgrade = Crown of Chaos
- [x] All the crowns' immunities, plus the Crown of Void's teleport dodge
- [x] Netherite helmet stats from the start; no netherite upgrade (the item exists, creative tab only)
- [x] Primary ability "The pain of death": Arrow Volley, Wither Skull, Fire Charge and Lightning Strike at once
- [x] Secondary ability "Death's door": Darkness Pulse lasting 300% longer
- [x] Can command every unit type
- [ ] Art: "glitched" sprite combining all the crown sprites
- [x] Art: 3D model

## Art and 3D models

- [x] 3D model: Golden Crown
- [x] 3D model: Crown of Darkness
- [x] 3D model: Crown of Sun
- [x] 3D model: Crown of Winter
- [x] 3D model: Crown of Iron
- [x] 3D model: Crown of Void
- [ ] Faction Flag art (it's a vanilla banner for now)
- [ ] Menu art for the faction, unit command and Commander's Table menus (optional; they're drawn in a flat style now)
- [ ] Icicle: switch to the vanilla icicle once the mod moves to Minecraft 26.4 (it's a Packed Ice shard now)

## Testing

Two players (LAN with a second account):
- [ ] Faction invites and join requests
- [ ] Kicking a member, and handing over ownership
- [ ] Flags of different factions must be 200 blocks apart
- [ ] Another faction's territory title ("Faction Territory")
- [ ] War between two real factions, and units fighting each other

One player:
- [ ] Pressing J opens the faction menu
- [ ] Crafting a Faction Flag
- [ ] Unit command menu (now a HUD overlay): number keys 1-7 give orders without switching the hotbar, V closes it, camera and movement stay free
- [ ] Every unit recipe at the Commander's Table (Polar Bear, Ravager, Enderman and Shulker have been tried)
- [ ] The Targeted stance on its own (without red alert)
- [ ] Changing a faction's color while its units are out
- [ ] Units after reloading the world

## Gameplay issues

- [x] Units don't burn in daylight anymore (wild mobs still do)
- [x] Warden units never give Darkness, and always stay above ground (they used to dig away and vanish)
- [x] Piglin and Warden units now fight the enemies the faction rules pick (red alert, Targeted, war, Attack orders)
- [ ] Check that Hoglin units do too (same fix, not tried in game yet)
- [x] Placed flags and Faction Flag items repaint when a faction changes color
- Left as is: flags removed by commands or other mods drop a plain banner (the claim is still removed)
- Not an issue after all: units already spot targets as far up and down as sideways (only line of sight blocks them)

## Housekeeping

- [ ] Crown Ideas.md: cross off the Crown of Iron and fill in its crafting materials
- [ ] Crown Ideas.md: add the Crown of Void
- [ ] Crown Ideas.md: mark the icicle idea as done (it's Icicle Drop)
- [ ] Bump the version number (gradle.properties) before sharing builds

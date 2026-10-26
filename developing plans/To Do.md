# To Do

What's left for The Crown of Chaos, as of 25 October 2026. Tick items off with `[x]`.

## First thing next session

- [x] Unit command menu (V): don't lock the camera or movement while it's open, so players can
      keep fighting. Changing the held item only by scroll wheel while it's open is fine.
      (Today it's a normal menu screen, which takes over the mouse and keyboard; it likely needs
      to become an on-screen overlay that keeps the game in control, with the number keys still
      giving orders.)

## The Crown of Chaos (last crown in the plan)

Built and in the game:

- [x] Incomplete Crown of Chaos: crafted from all nine crowns; can't be worn or used
- [x] Soul of Chaos: the boss's item drop, a material used only for the Crown of Chaos
- [x] Chaos Arena dimension: a round floating arena (about 73 blocks across) in a dark purple void, glitched floor, Herobrine altar (lighting it starts the fight), no natural mobs or treasure, a wall of lava that never flows with a bedrock shell round it
- [x] Glitched portal: throw Disc 11 into a nether portal in the Overworld; the frame becomes "null" blocks and the obsidian random blocks; breaking the null blocks destroys the whole portal
- [x] Exit portal in the arena, which opens only once the boss is beaten and takes you back to where you came in
- [x] Herobrine, The Chaos King: a player-style boss in Warfist's skin with the Crown of Chaos; the altar spawns him in the fire with lightning, he floats up, four waves of units (sized by the players there when the altar is lit, with a catch-up for anyone who arrives later), then he lands with an enchanted-looking sword and fights with melee and a Wither Skull, Fire Charge or Lightning Strike every 12 seconds
- [x] His banners, his evil laugh (waves, player deaths, ability hits), his death wail, the fight music "Empire" and, once he falls, a VICTORY banner and the victory music "Celebration"
- [x] Chaos smithing template ("Chaos Upgrade"): made by throwing a Netherite Upgrade smithing template into the soul fire that rises on the altar after the boss is beaten. This is the only way to get it, by design
- [x] Smithing recipe: Incomplete Crown of Chaos + Soul of Chaos + Chaos Upgrade = Crown of Chaos
- [x] All the crowns' immunities, plus the Crown of Void's teleport dodge
- [x] Netherite helmet stats from the start; no netherite upgrade (the item exists, creative tab only)
- [x] Primary ability "The pain of death": Arrow Volley, Wither Skull, Fire Charge and Lightning Strike at once
- [x] Secondary ability "Death's door": Darkness Pulse lasting 300% longer
- [x] Can command every unit type
- [x] Art: 3D model
- [x] The lore book "A Warning from a Former King": given the first time you have a Golden Crown, pages unlock with advancements, craftable from a Golden Crown and a book
- [x] The advancement tab "The Crown of Chaos": the nine crowns, all of them, netherite crowns, and the path to the finished crown

Still open:

- [ ] Tune the Chaos King fight in play (health, damage, wave sizes, his lines, which are placeholders)

Art decisions (Warfist, 25 October 2026): the "null" block stays Minecraft's missing-texture look, the Crown of Chaos sprite is final, and the menus' colors follow the Crown of Darkness, the mod's theme.

Decided (Warfist, 25 October 2026): there are no rules inside the arena, so people can fight the boss however they like (placing and breaking blocks, ender pearls, beds). The laugh and the death wail are royalty free and need no credit; only the two songs are credited.

## Art and 3D models

- [x] 3D model: Golden Crown
- [x] 3D model: Crown of Darkness
- [x] 3D model: Crown of Sun
- [x] 3D model: Crown of Winter
- [x] 3D model: Crown of Iron
- [x] 3D model: Crown of Void
- [x] Faction Flag art: it stays a vanilla banner on purpose, so players make it their own in the vanilla loom
- [x] Menu art for the faction, unit command and Commander's Table menus: final, in the Crown of Darkness's teal and cyan
- [ ] Icicle: switch to the vanilla icicle once the mod moves to Minecraft 26.4 (it's a Packed Ice shard now)

## Testing

Two players (LAN with a second account):
- [ ] A full Chaos King fight with two or more players, including someone arriving partway through (the catch-up) and the Warden wave; to be played again with the newest changes (Warfist, 25 October 2026)
- [x] Faction invites and join requests
- [x] Kicking a member, and handing over ownership
- [x] Flags of different factions must be 200 blocks apart
- [x] Another faction's territory title ("Faction Territory")
- [ ] War between two real factions, and units fighting each other

One player:
- [x] Poseidon's Regeneration switching off again after you leave water
- [x] The Faction Flag in a real loom screen (its preview draws the base white)
- [x] The Chaos King's laugh, death wail and victory music heard in a real fight
- [x] Relighting the altar after a win or a wipe, and reloading the world mid-fight
- [x] Pressing J opens the faction menu
- [x] Crafting a Faction Flag
- [x] Unit command menu (now a HUD overlay): number keys 1-7 give orders without switching the hotbar, V closes it, camera and movement stay free
- [x] Every unit recipe at the Commander's Table (Polar Bear, Ravager, Enderman and Shulker have been tried)
- [x] The Targeted stance on its own (without red alert)
- [x] Changing a faction's color while its units are out
- [x] Units after reloading the world

## Gameplay issues

- [x] Units don't burn in daylight anymore (wild mobs still do)
- [x] Warden units never give Darkness, and always stay above ground (they used to dig away and vanish)
- [x] Piglin and Warden units now fight the enemies the faction rules pick (red alert, Targeted, war, Attack orders)
- [x] Check that Hoglin units do too (same fix, not tried in game yet)
- [x] Placed flags and Faction Flag items repaint when a faction changes color
- Left as is: flags removed by commands or other mods drop a plain banner (the claim is still removed)
- Not an issue after all: units already spot targets as far up and down as sideways (only line of sight blocks them)

## Housekeeping

- [ ] The version number (gradle.properties) stays 1.0.0 until the first official release (Warfist, 25 October 2026); don't bump it before then. The idea that release was waiting on was the lore book, which is done (26 October 2026)

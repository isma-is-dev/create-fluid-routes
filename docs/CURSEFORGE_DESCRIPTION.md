# CurseForge description (copy/paste)

BBCode, ready for the project description editor.

---

[b]See your whole pipe network on Xaero's World Map.[/b]

Create's fluid pipes are easy to build and impossible to audit once a base grows. Fluid Routes mirrors them onto Xaero's World Map:

[list]
[*][b]Pipe lines[/b] - a single continuous blue line with a dark outline, following the real pipe openings.
[*][b]Nodes[/b] - cyan mechanical pumps, blue fluid tanks, violet spouts and grey-blue isolated pipes.
[*][b]Connection accurate[/b] - routing comes from Create's own [i]FluidPipeBlock.isOpenAt[/i], so casings, brackets, tees and capped ends behave exactly like in game.
[*][b]On-map toggle[/b] - a Fluid button with a live pump counter.
[/list]

[h2]No blocks. No items. No worldgen.[/h2]

A pure viewer: it reads block entity data on the server and streams it to clients with the map open. Safe to add to an existing world and safe to remove.

[h2]Performance first[/h2]

[list]
[*]Nothing is collected while no one has the map open.
[*]Chunk load/unload indexing, no world scans.
[*]Textures rebuild only when the pipe graph changes.
[*]Packets are chunked and hard-capped by config.
[/list]

[h2]Requirements[/h2]

[list]
[*]Minecraft 1.21.1
[*]NeoForge 21.1.x
[*]Create 6.0.10+ (6.0.x)
[*]Xaero's World Map 1.40.x (optional - without it the mod does nothing)
[/list]

[h2]Stacks with the other route mods[/h2]

Postal Routes and Stress Routes use different button positions and colours; all three can run together.

[h2]Credits[/h2]

Made by isma-is-dev. Not affiliated with Mojang, Create or Xaero.

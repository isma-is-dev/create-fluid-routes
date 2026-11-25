# Create: Fluid Routes

**See your whole pipe network on Xaero's World Map.** Pipes, pumps, tanks and spouts drawn
as one continuous blue line, with connection-accurate routing.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-2f8d46)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-f16436)](https://neoforged.net)
[![Create](https://img.shields.io/badge/Create-6.0.10+-f0b23c)](https://createmod.com)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## What it does

* **Pipe lines** — a single continuous blue line with a dark outline, following the actual
  pipe openings (casings, brackets and tees included).
* **Nodes** — pumps (cyan), fluid tanks (blue), spouts (violet) and isolated pipes
  (grey-blue).
* **No blocks, no items, no worldgen.** A pure viewer, safe to add and remove.
* **Multiplayer friendly** — install on server and clients; only loaded chunks are visible.

Connections come from Create's own `FluidPipeBlock.isOpenAt`, so a capped pipe or a pipe
pointing at a solid block is not drawn as connected.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| Create | 6.0.10 or newer (6.0.x) |
| Xaero's World Map | 1.40.x (optional at runtime) |

## Installation

1. NeoForge 21.1.x for Minecraft 1.21.1.
2. Install Create and Xaero's World Map.
3. Drop `fluidroutes-1.21.1-1.0.0.jar` into `mods/`.
4. Open the map (`M`) and press the **Fluid** button on the left.

## Reading the map

| Element | Meaning |
|---|---|
| Continuous blue line, dark outline | Connected pipe run |
| Cyan marker | Mechanical pump |
| Blue marker | Fluid tank |
| Violet marker | Spout |
| Grey-blue marker | Pipe with no connections |

Hover a marker for the block name and its role.

## Configuration

`config/fluidroutes-client.toml`

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch (also the map button) |
| `showRoutes` | `true` | Draw pipe lines |
| `showNodes` | `true` | Draw pump/tank/spout markers |
| `lineTheme` | `BLUE` | `BLUE`, `CYAN` or `TEAL` |
| `routeOpacity` | `100` | Line opacity, 0-100 |
| `dashedRoutes` | `false` | Solid single line by default |
| `maxMapSections` | `512` | 128×128 block texture cache limit |
| `widgetY` | `80` | Button position (Postal 48, Stress 64) |

`config/fluidroutes-server.toml`

| Option | Default | Description |
|---|---|---|
| `syncIntervalTicks` | `10` | Rebuild rate while a map is open |
| `maxNodes` | `16384` | Pipe/pump/tank blocks per dimension |
| `maxDirtyChunksPerTick` | `4` | Chunk rescans per tick after block changes |

Commands: `/fluidroutes stats` and `/fluidroutes rescan` (permission level 2).

## Performance

* Nothing is collected while no map is open.
* Chunk load/unload indexing, no world scans.
* Textures rebuild only when the pipe graph changes.
* Packets are chunked (192 entries each) and hard-capped by config.

## Compatibility

Tested with NeoForge 21.1.228, Create 6.0.10 and Xaero's World Map 1.40.11. No Create
mixins: public block entity API and `FluidPipeBlock` helpers only. The only mixin targets
Xaero's map and is skipped when Xaero is absent.

## Building

```powershell
powershell -ExecutionPolicy Bypass -File tools/fetch-libs.ps1
powershell -ExecutionPolicy Bypass -File tools/build.ps1
powershell -ExecutionPolicy Bypass -File tools/install-to-instance.ps1
```

## License

MIT — see [LICENSE](LICENSE). Not affiliated with Mojang, Create or Xaero.

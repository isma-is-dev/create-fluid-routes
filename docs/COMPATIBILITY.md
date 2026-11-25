# Compatibility

## Target environment

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228 |
| Create | 1.21.1-6.0.10 |
| Xaero's World Map | 1.40.11 |
| Mod count | ~190 (Create-focused pack) |

## Why it is safe

The mod never mixins into Create. It reads public API only:

* `FluidPipeBlock.isPipe` / `isOpenAt` for pipe connectivity.
* Block entity types for pumps, tanks and spouts.
* No registry entries, no world data, no recipes.

Safe alongside pipe-heavy Create addons (Diesel Generators, New Age, Crafts & Additions,
Central Kitchen, Garnished, Create: Connected, etc.): pipes are pipes regardless of which
addon placed them.

## Known interactions

| Situation | Behaviour |
|---|---|
| Xaero's World Map absent | Mod loads, does nothing; the mixin is skipped. |
| Xaero update moves the render hook | Overlay stops drawing, no crash (`require = 0`). |
| Create 5.x | Mod refuses to load (declared dependency). |
| Server without the mod | Client shows nothing, no errors. |
| Very large pipe network (> 16384 blocks) | Capped by `server.maxNodes`; raise it if needed. |
| Pipes with casings/brackets | Only open sides are drawn, matching Create. |

## With the sibling mods

Postal Routes (y=48), Stress Routes (y=64) and Fluid Routes (y=80) stack on the map. Each
has its own toggle and config.

# Design notes — Fluid Routes

What Create exposes for fluid pipes and why the overlay is built this way. Verified against
`create-1.21.1-6.0.10.jar`.

## What Create exposes

```
com.simibubi.create.content.fluids.pipes.FluidPipeBlock
    public static boolean isPipe(BlockState state);
    public static boolean isOpenAt(BlockState state, Direction direction);

com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity   // every pipe
com.simibubi.create.content.fluids.pump.PumpBlockEntity         // mechanical pump
com.simibubi.create.content.fluids.tank.FluidTankBlockEntity    // tank (multi-block)
com.simibubi.create.content.fluids.spout.SpoutBlockEntity       // spout
```

Pipe connectivity is stored in the block state as one boolean property per direction
(`PROPERTY_BY_DIRECTION`). `isOpenAt` reads it, which is exactly what Create's own
propagation uses, so capped pipes, casings and brackets are respected.

`FluidNetwork`/`FluidPropagator` are transient propagation helpers: they are created while
fluid moves and do not survive as a queryable graph. The block-state openings are the
stable source of truth for a map.

## Data flow

```
server                                          client
chunk index of pipe/pump/tank/spout BEs          FluidClientState (edges + nodes)
  └─ every pipe: for each open side                └─ FluidTextureCache (128x128 tiles)
       ├─ neighbour pipe open back? -> edge             └─ FluidMapOverlay (mixin render)
       ├─ neighbour pump/tank/spout? -> edge
       └─ pipes with no edges become a marker
```

Edges are emitted once per pair (lower coordinate first). Pipes are geometry, pumps, tanks
and spouts are nodes.

## Two hashes

`topologyHash` and `geometryHash` are identical today (there are no continuously changing
stats in the payload), but both are carried so a future "tank fill" or "flow rate" field can
be added without reworking the client's texture invalidation.

## Rendering

Same transform and raster approach as the other route mods: one texture pixel per block,
1 px halo drawn in a pass before the line, 128x128 sections cached and released when they go
out of use. Lines are solid by default (`dashedRoutes = false`).

## Limits

1. **Loaded chunks only.** Pipes outside simulation distance are invisible until they load.
2. **No fluid contents.** The overlay shows the network, not what is inside it: tank fill and
   flow direction are not part of the drawing.
3. **Pump direction is not shown.** A pump is a marker; the arrow would need a per-pump
   facing field, which is cheap to add later.
4. **Multi-block tanks** are indexed per block entity; a large tank shows one marker at its
   controller block and small ones at the rest, which reads fine at map scale.

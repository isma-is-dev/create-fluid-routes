# Testing checklist

## 1. Loads

`logs/latest.log` should contain `Create: Fluid Routes 1.0.0 (fluidroutes)`.

## 2. Hook wired

Open the world map once:

```
Xaero's World Map found; postal map integration enabled
Fluid Routes: Xaero world map hook is active
```

## 3. Widget

A **Fluid** button appears below the Stress one. The number is the amount of mechanical
pumps in the viewed dimension.

## 4. Data

```
/give @p create:fluid_pipe 16
/give @p create:mechanical_pump
/give @p create:fluid_tank 2
```

Lay a pipe run, cap it with a pump and a tank. `/fluidroutes stats` should report blocks,
pipe links, nodes and pumps. `/fluidroutes rescan` re-indexes chunks manually.

## 5. Alignment

One map pixel is one block with Xaero's camera transform; markers must sit exactly on their
blocks.

## 6. Multiplayer

Server without the mod → client logs one line and stops asking. Server with it → only loaded
chunks are shown.

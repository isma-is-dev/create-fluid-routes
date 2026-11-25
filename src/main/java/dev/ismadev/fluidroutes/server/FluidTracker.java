package dev.ismadev.fluidroutes.server;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.ismadev.fluidroutes.FluidConfig;
import dev.ismadev.fluidroutes.FluidRoutes;
import dev.ismadev.fluidroutes.data.FluidEdge;
import dev.ismadev.fluidroutes.data.FluidNode;
import dev.ismadev.fluidroutes.data.FluidSnapshot;
import dev.ismadev.fluidroutes.data.NodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks Create's fluid pipes, pumps, tanks and spouts of one server level and rebuilds the
 * pipe graph on demand.
 *
 * <p>Connections come from the pipe block state itself: a pipe is only drawn towards a side
 * where {@link FluidPipeBlock#isOpenAt} says it is open, and neighbouring pipes must be open
 * back. That keeps casings, brackets and tees accurate.</p>
 */
public final class FluidTracker {

    private final ServerLevel level;

    private final Map<ChunkPosKey, Set<BlockPos>> byChunk = new HashMap<>();
    private final Set<BlockPos> positions = new HashSet<>();
    private final Set<ChunkPosKey> dirty = new HashSet<>();

    private FluidSnapshot cachedSnapshot;
    private long cachedTick = Long.MIN_VALUE;
    private boolean seeded;
    private int ambientTicks;

    public FluidTracker(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel level() {
        return level;
    }

    public int trackedBlocks() {
        return positions.size();
    }

    // ------------------------------------------------------------------
    // Discovery
    // ------------------------------------------------------------------

    public void onChunkLoad(LevelChunk chunk) {
        if (chunk.getLevel() != level) {
            return;
        }
        scan(chunk);
    }

    public void onChunkUnload(LevelChunk chunk) {
        ChunkPosKey key = ChunkPosKey.of(chunk.getPos());
        Set<BlockPos> old = byChunk.remove(key);
        if (old != null) {
            positions.removeAll(old);
        }
        dirty.remove(key);
        invalidate();
    }

    public void markDirty(BlockPos pos) {
        dirty.add(ChunkPosKey.of(pos));
        invalidate();
    }

    public void processDirty() {
        ambientTicks++;
        if (ambientTicks >= 20) {
            ambientTicks = 0;
            rescanAroundPlayers(3);
        }
        if (dirty.isEmpty()) {
            return;
        }
        int budget = FluidConfig.maxDirtyChunksPerTick();
        Iterator<ChunkPosKey> iterator = dirty.iterator();
        while (iterator.hasNext() && budget-- > 0) {
            ChunkPosKey key = iterator.next();
            iterator.remove();
            LevelChunk chunk = level.getChunkSource().getChunkNow(key.x(), key.z());
            if (chunk != null) {
                scan(chunk);
            }
        }
    }

    public int rescanAroundPlayers(int radius) {
        int scanned = 0;
        try {
            for (ServerPlayer player : level.players()) {
                scanned += rescanArea(player.chunkPosition(), radius);
            }
        } catch (Throwable t) {
            FluidRoutes.LOGGER.debug("Could not rescan fluid chunks in {}", level.dimension().location(), t);
        }
        return scanned;
    }

    public int rescanArea(ChunkPos center, int radius) {
        int scanned = 0;
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                    if (chunk != null) {
                        scan(chunk);
                        scanned++;
                    }
                }
            }
        } catch (Throwable t) {
            FluidRoutes.LOGGER.debug("Could not rescan fluid chunks in {}", level.dimension().location(), t);
        }
        return scanned;
    }

    private void scan(LevelChunk chunk) {
        Set<BlockPos> found = null;
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (isTracked(entry.getValue())) {
                if (found == null) {
                    found = new HashSet<>();
                }
                found.add(entry.getKey().immutable());
            }
        }

        ChunkPosKey key = ChunkPosKey.of(chunk.getPos());
        Set<BlockPos> old = byChunk.remove(key);
        if (old != null) {
            positions.removeAll(old);
        }
        if (found != null) {
            byChunk.put(key, found);
            positions.addAll(found);
        }
        invalidate();
    }

    private static boolean isTracked(BlockEntity be) {
        return be instanceof FluidPipeBlockEntity
            || be instanceof PumpBlockEntity
            || be instanceof FluidTankBlockEntity
            || be instanceof SpoutBlockEntity;
    }

    private void invalidate() {
        cachedTick = Long.MIN_VALUE;
        cachedSnapshot = null;
    }

    // ------------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------------

    public FluidSnapshot snapshot() {
        long tick = level.getServer() == null ? level.getGameTime() : level.getServer().getTickCount();
        if (cachedSnapshot != null && cachedTick == tick) {
            return cachedSnapshot;
        }

        seedIfNeeded();

        List<FluidEdge> edges = new ArrayList<>();
        List<FluidNode> nodes = new ArrayList<>();
        Set<BlockPos> connectedPipes = new HashSet<>();
        int pumps = 0;
        int maxNodes = FluidConfig.maxNodes();

        List<BlockPos> ordered = new ArrayList<>(positions.size());
        ordered.addAll(positions);
        ordered.sort(Comparator.comparingLong(BlockPos::asLong));

        int visited = 0;
        for (BlockPos pos : ordered) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                positions.remove(pos);
                continue;
            }
            if (!isTracked(be)) {
                positions.remove(pos);
                continue;
            }
            if (visited++ >= maxNodes) {
                continue;
            }

            if (be instanceof FluidPipeBlockEntity) {
                BlockState state = be.getBlockState();
                for (Direction direction : Direction.values()) {
                    if (!FluidPipeBlock.isOpenAt(state, direction)) {
                        continue;
                    }
                    BlockPos neighbourPos = pos.relative(direction);
                    // Emit each pipe-pipe edge once, from the lower coordinate side.
                    if (neighbourPos.asLong() <= pos.asLong()) {
                        continue;
                    }
                    if (!level.isLoaded(neighbourPos)) {
                        continue;
                    }
                    BlockState neighbourState = level.getBlockState(neighbourPos);
                    BlockEntity neighbour = level.getBlockEntity(neighbourPos);
                    if (FluidPipeBlock.isPipe(neighbourState)) {
                        if (!FluidPipeBlock.isOpenAt(neighbourState, direction.getOpposite())) {
                            continue;
                        }
                    } else if (!(neighbour instanceof PumpBlockEntity
                        || neighbour instanceof FluidTankBlockEntity
                        || neighbour instanceof SpoutBlockEntity)) {
                        continue;
                    }
                    edges.add(new FluidEdge(pos, neighbourPos, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F));
                    connectedPipes.add(pos);
                    connectedPipes.add(neighbourPos);
                }
            } else if (be instanceof PumpBlockEntity) {
                pumps++;
                nodes.add(new FluidNode(pos, 0.5F, 0.5F, 0.5F, NodeKind.PUMP, label(be, pos)));
            } else if (be instanceof FluidTankBlockEntity) {
                nodes.add(new FluidNode(pos, 0.5F, 0.5F, 0.5F, NodeKind.TANK, label(be, pos)));
            } else if (be instanceof SpoutBlockEntity) {
                nodes.add(new FluidNode(pos, 0.5F, 0.5F, 0.5F, NodeKind.SPOUT, label(be, pos)));
            }
        }

        // Isolated pipes would otherwise be invisible: give them a small marker.
        for (BlockPos pos : ordered) {
            if (level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity
                && !connectedPipes.contains(pos)) {
                nodes.add(new FluidNode(pos, 0.5F, 0.5F, 0.5F, NodeKind.PIPE, label(level.getBlockEntity(pos), pos)));
            }
        }

        int topologyHash = hash(edges, nodes, pumps);
        int geometryHash = hash(edges, nodes, pumps);
        FluidSnapshot snapshot = new FluidSnapshot(level.dimension(), topologyHash, geometryHash,
            List.copyOf(edges), List.copyOf(nodes), pumps);
        cachedSnapshot = snapshot;
        cachedTick = tick;
        return snapshot;
    }

    private static String label(BlockEntity be, BlockPos pos) {
        try {
            return be.getBlockState().getBlock().getDescriptionId();
        } catch (Throwable t) {
            return "";
        }
    }

    private void seedIfNeeded() {
        if (seeded) {
            return;
        }
        seeded = true;
        rescanAroundPlayers(12);
    }

    private static int hash(List<FluidEdge> edges, List<FluidNode> nodes, int pumpCount) {
        int hash = 1 + pumpCount * 131;
        for (FluidEdge edge : edges) {
            hash ^= 31 * hashPos(edge.from()) + hashPos(edge.to());
        }
        for (FluidNode node : nodes) {
            hash ^= 17 * hashPos(node.pos()) + node.kind() * 7 + node.label().hashCode();
        }
        return hash;
    }

    private static int hashPos(BlockPos pos) {
        return pos.getX() * 73856093 ^ pos.getY() * 19349663 ^ pos.getZ() * 83492791;
    }

    /** Chunk position key without relying on mutable fastutil types in maps. */
    public record ChunkPosKey(int x, int z) {

        public static ChunkPosKey of(ChunkPos pos) {
            return new ChunkPosKey(pos.x, pos.z);
        }

        public static ChunkPosKey of(BlockPos pos) {
            return new ChunkPosKey(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }
}

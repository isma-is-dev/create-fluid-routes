package dev.ismadev.fluidroutes.client;

import dev.ismadev.fluidroutes.data.FluidEdge;
import dev.ismadev.fluidroutes.data.FluidNode;
import dev.ismadev.fluidroutes.net.S2CTopologyPart;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side mirror of the kinetic network. Mutations happen on the client thread.
 */
public final class FluidClientState {

    /** Dimension currently displayed by Xaero's world map, updated by the overlay. */
    @Nullable
    public static ResourceKey<Level> viewedDimension;

    private static final Map<ResourceKey<Level>, DimData> DIMENSIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, PendingTopology> PENDING = new HashMap<>();

    private FluidClientState() {
    }

    public static DimData get(ResourceKey<Level> dimension) {
        return DIMENSIONS.computeIfAbsent(dimension, key -> new DimData());
    }

    @Nullable
    public static DimData getIfPresent(ResourceKey<Level> dimension) {
        return DIMENSIONS.get(dimension);
    }

    public static void clear() {
        DIMENSIONS.clear();
        PENDING.clear();
    }

    public static void acceptTopology(S2CTopologyPart part) {
        ResourceKey<Level> dimension = part.dimension();
        PendingTopology pending = PENDING.get(dimension);
        if (pending == null || pending.requestId != part.requestId() || pending.parts != part.parts()) {
            pending = new PendingTopology(part.requestId(), part.parts());
            PENDING.put(dimension, pending);
        }

        pending.edges.addAll(part.edges());
        pending.nodes.addAll(part.nodes());
        pending.received++;

        if (pending.received >= pending.parts) {
            DimData data = get(dimension);
            data.geometryHash = part.geometryHash();
            data.edges = List.copyOf(pending.edges);
            data.nodes = List.copyOf(pending.nodes);
            data.pumpCount = part.pumpCount();
            PENDING.remove(dimension);
        }
    }

    public static final class PendingTopology {
        final int requestId;
        final int parts;
        int received;
        final List<FluidEdge> edges = new ArrayList<>();
        final List<FluidNode> nodes = new ArrayList<>();

        PendingTopology(int requestId, int parts) {
            this.requestId = requestId;
            this.parts = Math.max(1, parts);
        }
    }

    public static final class DimData {
        public int geometryHash;
        public List<FluidEdge> edges = List.of();
        public List<FluidNode> nodes = List.of();
        public int pumpCount;

        public boolean isEmpty() {
            return edges.isEmpty() && nodes.isEmpty();
        }
    }
}

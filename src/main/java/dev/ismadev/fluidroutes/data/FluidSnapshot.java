package dev.ismadev.fluidroutes.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Immutable view of one dimension's fluid network for a single server tick.
 *
 * @param topologyHash order independent hash of the drawing data
 * @param pumpCount    number of mechanical pumps found, shown on the map button
 */
public record FluidSnapshot(ResourceKey<Level> dimension,
                            int topologyHash,
                            int geometryHash,
                            List<FluidEdge> edges,
                            List<FluidNode> nodes,
                            int pumpCount) {

    public boolean isEmpty() {
        return edges.isEmpty() && nodes.isEmpty();
    }
}

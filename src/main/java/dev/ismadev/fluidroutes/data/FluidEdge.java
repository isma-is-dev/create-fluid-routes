package dev.ismadev.fluidroutes.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One edge of the kinetic graph: two blocks that rotation actually propagates between.
 *
 * <p>Coordinates are offsets from {@link #from}'s block corner, which keeps them small and
 * float-precise even far from the world origin.</p>
 */
public record FluidEdge(BlockPos from, BlockPos to,
                         float sx, float sy, float sz,
                         float ex, float ey, float ez) {

    public static final StreamCodec<FriendlyByteBuf, FluidEdge> STREAM_CODEC =
        StreamCodec.of(FluidEdge::write, FluidEdge::read);

    private static void write(FriendlyByteBuf buf, FluidEdge edge) {
        BlockPos.STREAM_CODEC.encode(buf, edge.from);
        BlockPos.STREAM_CODEC.encode(buf, edge.to);
        buf.writeFloat(edge.sx);
        buf.writeFloat(edge.sy);
        buf.writeFloat(edge.sz);
        buf.writeFloat(edge.ex);
        buf.writeFloat(edge.ey);
        buf.writeFloat(edge.ez);
    }

    private static FluidEdge read(FriendlyByteBuf buf) {
        return new FluidEdge(
            BlockPos.STREAM_CODEC.decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeList(FriendlyByteBuf buf, List<FluidEdge> edges) {
        buf.writeVarInt(edges.size());
        for (FluidEdge edge : edges) {
            STREAM_CODEC.encode(buf, edge);
        }
    }

    public static List<FluidEdge> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<FluidEdge> edges = new ArrayList<>(Math.min(size, 2048));
        for (int i = 0; i < size; i++) {
            edges.add(STREAM_CODEC.decode(buf));
        }
        return edges;
    }

    public double startX() {
        return from.getX() + sx;
    }

    public double startY() {
        return from.getY() + sy;
    }

    public double startZ() {
        return from.getZ() + sz;
    }

    public double endX() {
        return from.getX() + ex;
    }

    public double endY() {
        return from.getY() + ey;
    }

    public double endZ() {
        return from.getZ() + ez;
    }
}

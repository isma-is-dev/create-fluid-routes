package dev.ismadev.fluidroutes.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * A fluid network block on the map: pump, tank, spout or an isolated pipe.
 *
 * @param label block description id, translated client-side
 */
public record FluidNode(BlockPos pos, float dx, float dy, float dz,
                        int kind, String label) {

    public static final StreamCodec<FriendlyByteBuf, FluidNode> STREAM_CODEC =
        StreamCodec.of(FluidNode::write, FluidNode::read);

    private static void write(FriendlyByteBuf buf, FluidNode node) {
        BlockPos.STREAM_CODEC.encode(buf, node.pos);
        buf.writeFloat(node.dx);
        buf.writeFloat(node.dy);
        buf.writeFloat(node.dz);
        buf.writeByte(node.kind);
        buf.writeUtf(node.label, 128);
    }

    private static FluidNode read(FriendlyByteBuf buf) {
        return new FluidNode(
            BlockPos.STREAM_CODEC.decode(buf),
            buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readByte(),
            buf.readUtf(128));
    }

    public static void writeList(FriendlyByteBuf buf, List<FluidNode> nodes) {
        buf.writeVarInt(nodes.size());
        for (FluidNode node : nodes) {
            STREAM_CODEC.encode(buf, node);
        }
    }

    public static List<FluidNode> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<FluidNode> nodes = new ArrayList<>(Math.min(size, 512));
        for (int i = 0; i < size; i++) {
            nodes.add(STREAM_CODEC.decode(buf));
        }
        return nodes;
    }

    public double x() {
        return pos.getX() + dx;
    }

    public double y() {
        return pos.getY() + dy;
    }

    public double z() {
        return pos.getZ() + dz;
    }
}

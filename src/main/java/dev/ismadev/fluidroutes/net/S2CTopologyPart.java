package dev.ismadev.fluidroutes.net;

import dev.ismadev.fluidroutes.FluidRoutes;
import dev.ismadev.fluidroutes.client.FluidClientPayloadHandler;
import dev.ismadev.fluidroutes.data.FluidEdge;
import dev.ismadev.fluidroutes.data.FluidNode;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * One part of a fluid network snapshot. Large factories are split over several packets.
 */
public record S2CTopologyPart(int requestId, int part, int parts, int geometryHash,
                              ResourceKey<Level> dimension, int pumpCount,
                              List<FluidEdge> edges, List<FluidNode> nodes)
    implements CustomPacketPayload {

    public static final Type<S2CTopologyPart> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FluidRoutes.MODID, "topology"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTopologyPart> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.requestId);
            buf.writeVarInt(payload.part);
            buf.writeVarInt(payload.parts);
            buf.writeInt(payload.geometryHash);
            ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, payload.dimension);
            buf.writeVarInt(payload.pumpCount);
            FluidEdge.writeList(buf, payload.edges);
            FluidNode.writeList(buf, payload.nodes);
        },
        buf -> new S2CTopologyPart(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readInt(),
            ResourceKey.streamCodec(Registries.DIMENSION).decode(buf),
            buf.readVarInt(),
            FluidEdge.readList(buf),
            FluidNode.readList(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CTopologyPart payload, IPayloadContext context) {
        context.enqueueWork(() -> FluidClientPayloadHandler.handleTopology(payload));
    }
}

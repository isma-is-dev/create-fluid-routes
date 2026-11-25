package dev.ismadev.fluidroutes.net;

import dev.ismadev.fluidroutes.FluidRoutes;
import dev.ismadev.fluidroutes.server.FluidServerEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -&gt; server heartbeat sent while the world map is open. It carries the
 * dimension currently being viewed, so the server only streams relevant data.
 */
public record C2SRequestFluid(ResourceKey<Level> dimension) implements CustomPacketPayload {

    public static final Type<C2SRequestFluid> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FluidRoutes.MODID, "request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestFluid> STREAM_CODEC =
        StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), C2SRequestFluid::dimension,
            C2SRequestFluid::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SRequestFluid payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            FluidServerEvents.onRequest(player, payload.dimension());
        }
    }
}

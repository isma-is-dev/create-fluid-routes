package dev.ismadev.fluidroutes.net;

import dev.ismadev.fluidroutes.FluidRoutes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FluidNetwork {

    /** Bumped whenever a payload layout changes. */
    private static final String PROTOCOL_VERSION = "1";

    private FluidNetwork() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(FluidNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(C2SRequestFluid.TYPE, C2SRequestFluid.STREAM_CODEC, C2SRequestFluid::handle);
        registrar.playToClient(S2CTopologyPart.TYPE, S2CTopologyPart.STREAM_CODEC, S2CTopologyPart::handle);
    }

    public static String protocolVersion() {
        return PROTOCOL_VERSION;
    }
}

package dev.ismadev.fluidroutes.client;

import dev.ismadev.fluidroutes.net.S2CTopologyPart;

/**
 * Client-only payload sink. Kept separate so the payload classes can be loaded on a
 * dedicated server without touching client classes.
 */
public final class FluidClientPayloadHandler {

    private FluidClientPayloadHandler() {
    }

    public static void handleTopology(S2CTopologyPart payload) {
        FluidClientState.acceptTopology(payload);
    }
}

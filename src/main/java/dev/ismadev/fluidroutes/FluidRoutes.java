package dev.ismadev.fluidroutes;

import dev.ismadev.fluidroutes.net.FluidNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create: Fluid Routes.
 *
 * <p>Client-side visualisation of Create's package logistics on Xaero's World Map:
 * chain conveyor routes, frogports, postboxes and live package traffic. The mod adds
 * no blocks or items &mdash; it only reads Create's block entity data on the server and
 * mirrors it to the client for rendering.</p>
 */
@Mod(FluidRoutes.MODID)
public final class FluidRoutes {

    public static final String MODID = "fluidroutes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Create: Fluid Routes");

    public FluidRoutes(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FluidConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, FluidConfig.SERVER_SPEC);
        FluidNetwork.init(modBus);
    }
}

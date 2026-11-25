package dev.ismadev.fluidroutes;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for the fluid overlay.
 */
public final class FluidConfig {

    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();

        Pair<Server, ModConfigSpec> server = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
    }

    private FluidConfig() {
    }

    public enum LineTheme {
        /** Pipe blue, the default. */
        BLUE,
        /** Cyan, for players who want it closer to Create's copper pipes. */
        CYAN,
        /** Teal. */
        TEAL
    }

    private static <T> T safe(ModConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            T loaded = value.get();
            return loaded == null ? fallback : loaded;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean overlayEnabled() {
        return safe(CLIENT.enabled, true);
    }

    public static boolean showRoutes() {
        return safe(CLIENT.showRoutes, true);
    }

    public static boolean showNodes() {
        return safe(CLIENT.showNodes, true);
    }

    public static boolean dashedRoutes() {
        return safe(CLIENT.dashedRoutes, false);
    }

    public static LineTheme lineTheme() {
        return safe(CLIENT.lineTheme, LineTheme.BLUE);
    }

    public static int routeOpacity() {
        return Math.max(0, Math.min(100, safe(CLIENT.routeOpacity, 100)));
    }

    public static int maxMapSections() {
        return Math.max(16, safe(CLIENT.maxMapSections, 512));
    }

    public static int widgetY() {
        return Math.max(0, safe(CLIENT.widgetY, 96));
    }

    public static int syncIntervalTicks() {
        return Math.max(1, safe(SERVER.syncIntervalTicks, 10));
    }

    public static int maxNodes() {
        return Math.max(64, safe(SERVER.maxNodes, 16384));
    }

    public static int maxDirtyChunksPerTick() {
        return Math.max(1, safe(SERVER.maxDirtyChunksPerTick, 4));
    }

    public static final class Client {

        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.BooleanValue showRoutes;
        public final ModConfigSpec.BooleanValue showNodes;
        public final ModConfigSpec.BooleanValue dashedRoutes;
        public final ModConfigSpec.EnumValue<LineTheme> lineTheme;
        public final ModConfigSpec.IntValue routeOpacity;
        public final ModConfigSpec.IntValue maxMapSections;
        public final ModConfigSpec.IntValue widgetY;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("Visual settings for the fluid overlay on Xaero's World Map.").push("visuals");

            this.enabled = builder
                .comment("Master switch. Can also be toggled from the map widget.")
                .define("enabled", true);

            this.showRoutes = builder
                .comment("Draw the pipe network as lines.")
                .define("showRoutes", true);

            this.showNodes = builder
                .comment("Draw pumps, tanks and spouts as markers.")
                .define("showNodes", true);

            this.dashedRoutes = builder
                .comment("Dashed lines. Off by default: pipes read better as a single continuous line.")
                .define("dashedRoutes", false);

            this.lineTheme = builder
                .comment("Colour palette for the pipe network.")
                .defineEnum("lineTheme", LineTheme.BLUE);

            this.routeOpacity = builder
                .comment("Opacity of the pipe lines, 0-100.")
                .defineInRange("routeOpacity", 100, 0, 100);

            this.maxMapSections = builder
                .comment("Maximum number of 128x128 map sections kept in memory (64 KiB each).")
                .defineInRange("maxMapSections", 512, 16, 4096);

            this.widgetY = builder
                .comment("Vertical position of the Fluid button on the map, in GUI pixels.",
                    "Postal uses 48, Stress 64 and its heat button 80, so 96 keeps the column clear.")
                .defineInRange("widgetY", 96, 0, 400);

            builder.pop();
        }
    }

    public static final class Server {

        public final ModConfigSpec.IntValue syncIntervalTicks;
        public final ModConfigSpec.IntValue maxNodes;
        public final ModConfigSpec.IntValue maxDirtyChunksPerTick;

        Server(ModConfigSpec.Builder builder) {
            builder.comment("Server side scan limits for the fluid overlay.").push("scan");

            this.syncIntervalTicks = builder
                .comment("How often (in ticks) the pipe graph is rebuilt while a player has the map open.")
                .defineInRange("syncIntervalTicks", 10, 1, 100);

            this.maxNodes = builder
                .comment("Maximum number of pipe/pump/tank blocks reported per dimension.")
                .defineInRange("maxNodes", 16384, 128, 65536);

            this.maxDirtyChunksPerTick = builder
                .comment("Chunks rescanned per tick after a block placement or removal.")
                .defineInRange("maxDirtyChunksPerTick", 4, 1, 32);

            builder.pop();
        }
    }
}

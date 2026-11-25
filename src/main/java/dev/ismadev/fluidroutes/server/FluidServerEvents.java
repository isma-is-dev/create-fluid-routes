package dev.ismadev.fluidroutes.server;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ismadev.fluidroutes.FluidConfig;
import dev.ismadev.fluidroutes.FluidRoutes;
import dev.ismadev.fluidroutes.data.FluidEdge;
import dev.ismadev.fluidroutes.data.FluidNode;
import dev.ismadev.fluidroutes.data.FluidSnapshot;
import dev.ismadev.fluidroutes.net.S2CTopologyPart;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side glue: one {@link FluidTracker} per dimension and a demand driven feed for
 * players who have the map open.
 */
@EventBusSubscriber(modid = FluidRoutes.MODID)
public final class FluidServerEvents {

    private static final int REQUEST_TIMEOUT_TICKS = 100;
    /** Upper bound of edges+nodes per topology packet (~10 KiB). */
    private static final int ENTRIES_PER_PART = 192;

    private static final Map<ResourceKey<Level>, FluidTracker> TRACKERS = new HashMap<>();
    private static final Map<UUID, Request> REQUESTS = new HashMap<>();
    private static final Map<UUID, SentState> SENT = new HashMap<>();

    private static int nextRequestId = 1;

    private FluidServerEvents() {
    }

    private record Request(ResourceKey<Level> dimension, int tick) {
    }

    private record SentState(ResourceKey<Level> dimension, int hash) {
    }

    // ------------------------------------------------------------------
    // World indexing
    // ------------------------------------------------------------------

    public static FluidTracker trackerFor(ServerLevel level) {
        return TRACKERS.computeIfAbsent(level.dimension(), key -> new FluidTracker(level));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            trackerFor(level).onChunkLoad(chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            FluidTracker tracker = TRACKERS.get(level.dimension());
            if (tracker != null) {
                tracker.onChunkUnload(chunk);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            trackerFor(level).markDirty(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            trackerFor(level).markDirty(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TRACKERS.remove(level.dimension());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRACKERS.clear();
        REQUESTS.clear();
        SENT.clear();
    }

    // ------------------------------------------------------------------
    // Streaming
    // ------------------------------------------------------------------

    public static void onRequest(ServerPlayer player, ResourceKey<Level> dimension) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        REQUESTS.put(player.getUUID(), new Request(dimension, server.getTickCount()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();

        if (!REQUESTS.isEmpty()) {
            REQUESTS.entrySet().removeIf(entry -> now - entry.getValue().tick() > REQUEST_TIMEOUT_TICKS);
        }
        if (REQUESTS.isEmpty()) {
            SENT.clear();
            return;
        }

        int interval = FluidConfig.syncIntervalTicks();
        if (now % interval != 0) {
            return;
        }

        for (UUID viewer : new ArrayList<>(REQUESTS.keySet())) {
            Request request = REQUESTS.get(viewer);
            if (request == null) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(viewer);
            if (player == null || player.connection == null) {
                continue;
            }

            ResourceKey<Level> dimension = request.dimension();
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                continue;
            }

            FluidTracker tracker = trackerFor(level);
            tracker.processDirty();
            FluidSnapshot snapshot = tracker.snapshot();

            SentState sent = SENT.get(viewer);
            if (sent == null || sent.hash() != snapshot.topologyHash() || sent.dimension() != dimension) {
                sendTopology(player, snapshot);
                SENT.put(viewer, new SentState(dimension, snapshot.topologyHash()));
            }
        }
    }

    private static void sendTopology(ServerPlayer player, FluidSnapshot snapshot) {
        List<FluidEdge> edges = snapshot.edges();
        List<FluidNode> nodes = snapshot.nodes();
        int total = edges.size() + nodes.size();
        int parts = Math.max(1, (total + ENTRIES_PER_PART - 1) / ENTRIES_PER_PART);
        int requestId = nextRequestId++;

        int cursor = 0;
        for (int part = 0; part < parts; part++) {
            List<FluidEdge> partEdges = new ArrayList<>();
            List<FluidNode> partNodes = new ArrayList<>();
            while (cursor < total && partEdges.size() + partNodes.size() < ENTRIES_PER_PART) {
                if (cursor < edges.size()) {
                    partEdges.add(edges.get(cursor));
                } else {
                    partNodes.add(nodes.get(cursor - edges.size()));
                }
                cursor++;
            }
            PacketDistributor.sendToPlayer(player, new S2CTopologyPart(requestId, part, parts,
                snapshot.geometryHash(), snapshot.dimension(), snapshot.pumpCount(),
                partEdges, partNodes));
        }
    }

    // ------------------------------------------------------------------
    // Diagnostics
    // ------------------------------------------------------------------

    public static Component describe(MinecraftServer server) {
        MutableComponent output = Component.translatable("fluidroutes.command.stats.header",
            countTrackedBlocks(), REQUESTS.size());

        for (Map.Entry<ResourceKey<Level>, FluidTracker> entry : TRACKERS.entrySet()) {
            FluidTracker tracker = entry.getValue();
            FluidSnapshot snapshot;
            try {
                snapshot = tracker.snapshot();
            } catch (Throwable t) {
                FluidRoutes.LOGGER.warn("Failed to snapshot kinetic network in {}", entry.getKey().location(), t);
                continue;
            }
            output.append(Component.literal("\n")).append(Component.translatable(
                "fluidroutes.command.stats.dimension",
                entry.getKey().location().toString(),
                tracker.trackedBlocks(),
                snapshot.edges().size(),
                snapshot.nodes().size(),
                snapshot.pumpCount()));
        }

        return output.withStyle(ChatFormatting.GRAY);
    }

    private static int countTrackedBlocks() {
        int total = 0;
        for (FluidTracker tracker : TRACKERS.values()) {
            total += tracker.trackedBlocks();
        }
        return total;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fluidroutes")
            .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("stats").executes(context -> {
            context.getSource().sendSuccess(() -> describe(context.getSource().getServer()), false);
            return 1;
        }));

        root.then(Commands.literal("rescan").executes(context -> {
            MinecraftServer server = context.getSource().getServer();
            ChunkPos center = new ChunkPos(net.minecraft.core.BlockPos.containing(
                context.getSource().getPosition()));
            int chunks = 0;
            for (Map.Entry<ResourceKey<Level>, FluidTracker> entry : TRACKERS.entrySet()) {
                ServerLevel level = server.getLevel(entry.getKey());
                if (level != null) {
                    if (level.players().isEmpty()) {
                        chunks += entry.getValue().rescanArea(center, 12);
                    } else {
                        chunks += entry.getValue().rescanAroundPlayers(12);
                    }
                }
            }
            int scanned = chunks;
            context.getSource().sendSuccess(() -> Component.translatable(
                "fluidroutes.command.rescan", scanned).withStyle(ChatFormatting.GRAY), false);
            return 1;
        }));

        root.executes(context -> {
            context.getSource().sendSuccess(() -> describe(context.getSource().getServer()), false);
            return 1;
        });

        event.getDispatcher().register(root);
    }
}

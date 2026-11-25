package dev.ismadev.fluidroutes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ismadev.fluidroutes.FluidConfig;
import dev.ismadev.fluidroutes.FluidRoutes;
import dev.ismadev.fluidroutes.data.NodeKind;
import dev.ismadev.fluidroutes.data.FluidNode;
import dev.ismadev.fluidroutes.mixin.GuiMapAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the kinetic/fluid network on Xaero's World Map.
 */
public final class FluidMapOverlay {

    private static final double PICK_RADIUS = 3.5D;

    private static boolean failed;

    private FluidMapOverlay() {
    }

    public static void onRender(GuiGraphics graphics, GuiMap map, int mouseX, int mouseY, float partialTicks) {
        if (failed) {
            return;
        }
        try {
            GuiMapAccessor accessor = (GuiMapAccessor) map;
            MapProcessor processor = accessor.fluid$getMapProcessor();
            MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
            MapDimension mapDimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
            if (mapDimension == null) {
                return;
            }

            ResourceKey<Level> dimension = mapDimension.getDimId();
            if (dimension == null) {
                return;
            }
            FluidClientState.viewedDimension = dimension;

            FluidClientState.DimData data = FluidClientState.getIfPresent(dimension);
            int pumps = data == null ? 0 : data.pumpCount;
            FluidMapWidget.render(graphics, mouseX, mouseY, pumps);

            if (!FluidMapWidget.isEnabled()) {
                return;
            }
            if (data == null || data.isEmpty()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            double cameraX = accessor.fluid$getCameraX();
            double cameraZ = accessor.fluid$getCameraZ();
            double mapScale = accessor.fluid$getScale();
            if (!Double.isFinite(mapScale) || mapScale <= 0.0D) {
                return;
            }

            double guiScale = mc.getWindow().getGuiScale();
            if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
                guiScale = 1.0D;
            }
            double scale = mapScale / guiScale;
            if (!Double.isFinite(scale) || scale <= 0.0D) {
                return;
            }
            double factor = dimensionDivisor(processor, mapDimension, mc);
            if (!Double.isFinite(factor) || factor < 1.0E-4D) {
                factor = 1.0D;
            }

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate((float) map.width / 2.0F, (float) map.height / 2.0F, 0.0F);
            pose.scale((float) scale, (float) scale, 1.0F);
            pose.translate(-cameraX, -cameraZ, 0.0D);

            Rect2i bounds = visibleBounds(map.width, map.height, scale, cameraX, cameraZ);
            double mouseMapX = (mouseX - (double) map.width / 2.0D) / scale + cameraX;
            double mouseMapZ = (mouseY - (double) map.height / 2.0D) / scale + cameraZ;

            int opacity = Mth.clamp(FluidConfig.routeOpacity(), 0, 100);
            float alpha = opacity / 100.0F;

            FluidTextureCache.INSTANCE.ensure(dimension, data, factor,
                FluidConfig.showRoutes(), FluidConfig.showNodes(), FluidConfig.lineTheme());
            FluidTextureCache.INSTANCE.render(graphics, bounds, alpha);

            RenderSystem.enableBlend();
            FluidNode hovered = null;
            if (FluidConfig.showNodes()) {
                hovered = drawNodes(graphics, data, factor, scale, bounds, mouseMapX, mouseMapZ);
            }

            pose.popPose();
            graphics.flush();

            if (hovered != null) {
                List<Component> tooltip = tooltipFor(hovered);
                if (!tooltip.isEmpty()) {
                    graphics.renderComponentTooltip(mc.font, tooltip, mouseX, mouseY);
                }
            }
        } catch (Throwable t) {
            failed = true;
            FluidRoutes.LOGGER.error("Disabling the fluid map overlay after an unexpected error", t);
        }
    }

    private static Rect2i visibleBounds(int width, int height, double scale, double cameraX, double cameraZ) {
        int margin = 32;
        int x = Mth.floor((double) (-width) / 2.0D / scale + cameraX) - margin;
        int z = Mth.floor((double) (-height) / 2.0D / scale + cameraZ) - margin;
        int w = Mth.floor((double) width / scale) + margin * 2;
        int h = Mth.floor((double) height / scale) + margin * 2;
        return new Rect2i(x, z, w, h);
    }

    private static double dimensionDivisor(MapProcessor processor, MapDimension mapDimension, Minecraft mc) {
        try {
            Registry<DimensionType> registry = processor.getWorldDimensionTypeRegistry();
            if (registry != null && mc.player != null) {
                double div = mapDimension.calculateDimDiv(registry, mc.player.level().dimensionType());
                if (div > 1.0E-4D) {
                    return div;
                }
            }
        } catch (Throwable ignored) {
            // Identity fallback; drawing unshifted is better than not drawing.
        }
        return 1.0D;
    }

    @Nullable
    private static FluidNode drawNodes(GuiGraphics graphics, FluidClientState.DimData data, double factor,
                                        double scale, Rect2i bounds, double mouseMapX, double mouseMapZ) {
        FluidNode hovered = null;
        boolean detailed = scale >= 0.35D;

        for (FluidNode node : data.nodes) {
            double x = node.x() / factor;
            double z = node.z() / factor;
            if (!bounds.contains(Mth.floor(x), Mth.floor(z))) {
                continue;
            }

            FluidGlyphs.node(graphics, x, z, node.kind(), detailed);

            if (hovered == null
                && Math.abs(mouseMapX - x) <= PICK_RADIUS
                && Math.abs(mouseMapZ - z) <= PICK_RADIUS) {
                hovered = node;
            }
        }
        return hovered;
    }

    private static List<Component> tooltipFor(FluidNode node) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.translatable(node.label()).withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable(NodeKind.translationKey(node.kind())).withStyle(kindColor(node.kind())));
        return lines;
    }

    private static ChatFormatting kindColor(int kind) {
        return switch (kind) {
            case NodeKind.PUMP -> ChatFormatting.AQUA;
            case NodeKind.TANK -> ChatFormatting.BLUE;
            case NodeKind.SPOUT -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }
}

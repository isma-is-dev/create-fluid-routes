package dev.ismadev.fluidroutes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Tiny procedurally drawn markers for fluid blocks.
 */
public final class FluidGlyphs {

    private static final int OUTLINE = 0xC0081622;

    private FluidGlyphs() {
    }

    public static void node(GuiGraphics graphics, double x, double y, int kind, boolean detailed) {
        int size = switch (kind) {
            case 0 -> detailed ? 3 : 2; // pump
            case 1 -> detailed ? 3 : 2; // tank
            default -> detailed ? 2 : 1; // spout / isolated pipe
        };
        box(graphics, x, y, Math.max(1, size), color(kind), OUTLINE);
    }

    public static int color(int kind) {
        return switch (kind) {
            case 0 -> 0xFF35D0FF; // pump
            case 1 -> 0xFF2F7BFF; // tank
            case 2 -> 0xFFC08CFF; // spout
            default -> 0xFF6F8FB0; // isolated pipe
        };
    }

    public static void box(GuiGraphics graphics, double x, double y, int size, int fill, int outline) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x - size / 2.0D, y - size / 2.0D, 0.0D);
        graphics.fill(-1, -1, size + 1, size + 1, outline);
        graphics.fill(0, 0, size, size, fill);
        pose.popPose();
    }
}

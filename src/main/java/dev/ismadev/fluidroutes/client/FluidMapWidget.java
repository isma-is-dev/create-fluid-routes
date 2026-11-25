package dev.ismadev.fluidroutes.client;

import dev.ismadev.fluidroutes.FluidConfig;
import dev.ismadev.fluidroutes.FluidRoutes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Map button for the fluid overlay. Its vertical position is configurable so Postal (48),
 * Stress (64) and Fluid (80) stack instead of overlapping.
 */
public final class FluidMapWidget {

    public static final int X = 3;
    public static final int WIDTH = 58;
    public static final int HEIGHT = 14;

    private static final int BACKGROUND_ON = 0xE0081420;
    private static final int BACKGROUND_OFF = 0xE0181818;
    private static final int BORDER_ON = 0xFF3D9BFF;
    private static final int BORDER_OFF = 0xFF5E5E5E;
    private static final int TEXT_ON = 0xFFD6EBFF;
    private static final int TEXT_OFF = 0xFF9A9A9A;

    private FluidMapWidget() {
    }

    public static boolean isEnabled() {
        return FluidConfig.overlayEnabled();
    }

    private static int y() {
        return FluidConfig.widgetY();
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY, int pumpCount) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        boolean enabled = isEnabled();
        int y = y();
        boolean hovered = isHovered(mouseX, mouseY);

        int background = enabled ? BACKGROUND_ON : BACKGROUND_OFF;
        int border = enabled ? (hovered ? 0xFF8CC8FF : BORDER_ON) : (hovered ? 0xFF8A8A8A : BORDER_OFF);

        graphics.fill(X, y, X + WIDTH, y + HEIGHT, background);
        graphics.fill(X, y, X + WIDTH, y + 1, border);
        graphics.fill(X, y + HEIGHT - 1, X + WIDTH, y + HEIGHT, border);
        graphics.fill(X, y, X + 1, y + HEIGHT, border);
        graphics.fill(X + WIDTH - 1, y, X + WIDTH, y + HEIGHT, border);

        FluidGlyphs.node(graphics, X + 9, y + 7, 0, true);

        graphics.drawString(font, Component.translatable("fluidroutes.widget.label"),
            X + 16, y + 3, enabled ? TEXT_ON : TEXT_OFF, false);

        if (enabled && pumpCount > 0) {
            String count = pumpCount > 99 ? "99+" : Integer.toString(pumpCount);
            int width = font.width(count);
            graphics.drawString(font, count, X + WIDTH - width - 4, y + 3, 0xFF35D0FF, false);
        }

        if (hovered) {
            graphics.renderComponentTooltip(font, tooltip(enabled), mouseX, mouseY);
        }
    }

    private static List<Component> tooltip(boolean enabled) {
        return List.of(
            Component.translatable("fluidroutes.widget.title").withStyle(ChatFormatting.AQUA),
            Component.translatable(enabled ? "fluidroutes.widget.on" : "fluidroutes.widget.off")
                .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY),
            Component.translatable("fluidroutes.widget.hint").withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    public static boolean isHovered(double mouseX, double mouseY) {
        int y = y();
        return mouseX >= X && mouseX < X + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) {
            return false;
        }
        toggle();
        return true;
    }

    public static void toggle() {
        try {
            boolean value = !FluidConfig.overlayEnabled();
            FluidConfig.CLIENT.enabled.set(value);
        } catch (Throwable t) {
            FluidRoutes.LOGGER.warn("Could not toggle the fluid overlay", t);
        }
    }
}

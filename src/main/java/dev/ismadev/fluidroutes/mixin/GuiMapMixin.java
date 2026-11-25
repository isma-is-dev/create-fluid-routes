package dev.ismadev.fluidroutes.mixin;

import dev.ismadev.fluidroutes.FluidRoutes;
import dev.ismadev.fluidroutes.client.FluidMapOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;

/**
 * Hooks the world map render right before its last GUI blit, which is where Create draws
 * its train map: above map tiles and map elements, below the map's own buttons.
 *
 * <p>{@code require = 0} is deliberate. If a future Xaero update moves that call, the
 * overlay silently does not render instead of crashing the game.</p>
 */
@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapMixin {

    @Unique
    private static boolean fluidroutes$loggedHook;

    @Unique
    private boolean fluidroutes$failed;

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
        ),
        require = 0
    )
    private void fluidroutes$renderFluidLayer(GuiGraphics graphics, int mouseX, int mouseY,
                                                float partialTicks, CallbackInfo ci) {
        if (fluidroutes$failed) {
            return;
        }
        if (!fluidroutes$loggedHook) {
            fluidroutes$loggedHook = true;
            FluidRoutes.LOGGER.info("Fluid Routes: Xaero world map hook is active");
        }
        try {
            FluidMapOverlay.onRender(graphics, (GuiMap) (Object) this, mouseX, mouseY, partialTicks);
        } catch (Throwable t) {
            fluidroutes$failed = true;
            FluidRoutes.LOGGER.error("Fluid Routes could not render on Xaero's World Map; "
                + "the overlay is disabled until the game restarts", t);
        }
    }
}

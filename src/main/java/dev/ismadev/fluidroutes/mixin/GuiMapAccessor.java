package dev.ismadev.fluidroutes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

/**
 * Read-only access to the private camera state of Xaero's world map.
 *
 * <p>Only applied when Xaero's World Map is installed (see {@link FluidMixinPlugin}).</p>
 */
@Mixin(value = GuiMap.class, remap = false)
public interface GuiMapAccessor {

    @Accessor("cameraX")
    double fluid$getCameraX();

    @Accessor("cameraZ")
    double fluid$getCameraZ();

    @Accessor("scale")
    double fluid$getScale();

    @Accessor("mapProcessor")
    MapProcessor fluid$getMapProcessor();
}

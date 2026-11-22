package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.Minecraft;

/**
 * Enables the stencil buffer of Minecraft's main render target at client setup. TACZ enables it the first time a scope
 * renders, which recreates the main color and depth buffers in the middle of that frame and shows one black frame.
 * Done before any world frame, the recreation is invisible. Client only, only call when TaczCompat.LOADED is true.
 */
public final class TaczEarlyStencil {

    private TaczEarlyStencil() {
    }

    /**
     * Enables the main render target's stencil buffer, once. Must run on the render thread.
     */
    public static void enable() {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

        if (target == null || target.isStencilEnabled())
            return;

        target.enableStencil();
        ModLogger.debug("stencil: enabled early");
    }
}

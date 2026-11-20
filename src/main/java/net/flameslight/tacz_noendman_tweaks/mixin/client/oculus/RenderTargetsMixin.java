package net.flameslight.tacz_noendman_tweaks.mixin.client.oculus;

import net.flameslight.tacz_noendman_tweaks.compat.oculus.OculusDhDepthGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects Iris re-attaching the main depth texture to its framebuffers, for the DH depth guard. Only applies with
 * Oculus.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.targets.RenderTargets", remap = false)
public abstract class RenderTargetsMixin {

    @Shadow
    private int cachedDepthBufferVersion;

    @Unique
    private int tacz_noendman$oldVersion;

    /**
     * Stores the depth buffer version before the resize check.
     *
     * @param cir the callback info
     */
    @Inject(method = "resizeIfNeeded", at = @At("HEAD"))
    private void tacz_noendman$beforeResize(CallbackInfoReturnable<Boolean> cir) {
        this.tacz_noendman$oldVersion = this.cachedDepthBufferVersion;
    }

    /**
     * When the resize check re-attached a new main depth texture, requests the DH depth re-attach.
     *
     * @param cir the callback info
     */
    @Inject(method = "resizeIfNeeded", at = @At("RETURN"))
    private void tacz_noendman$afterResize(CallbackInfoReturnable<Boolean> cir) {
        if (this.cachedDepthBufferVersion != this.tacz_noendman$oldVersion)
            OculusDhDepthGuard.markReattached();
    }
}

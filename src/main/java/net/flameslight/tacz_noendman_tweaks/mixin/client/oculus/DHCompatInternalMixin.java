package net.flameslight.tacz_noendman_tweaks.mixin.client.oculus;

import net.flameslight.tacz_noendman_tweaks.compat.oculus.OculusDhDepthGuard;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces Oculus to re-attach the Distant Horizons depth texture to its DH framebuffers after Iris replaced it with
 * the main depth. Only applies with Oculus.
 *
 * This code was written in order to fix a bug when certain events (like guns with sight attachment) causing
 * DH mod to break and load its load in-front of rendered chunks.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.dh.DHCompatInternal", remap = false)
public abstract class DHCompatInternalMixin {

    @Shadow
    private int storedDepthTex;

    /**
     * After a main depth re-attach, makes Oculus's own check re-attach the DH depth.
     *
     * @param dhDepthTex the DH depth texture id
     * @param ci         the callback info
     */
    @Inject(method = "reconnectDHTextures", at = @At("HEAD"))
    private void tacz_noendman$forceReconnect(int dhDepthTex, CallbackInfo ci) {
        if (OculusDhDepthGuard.consume()) {
            this.storedDepthTex = -1;

            ModLogger.debugOnce("fix active: DH depth re-attach after a main depth change");
        }
    }
}

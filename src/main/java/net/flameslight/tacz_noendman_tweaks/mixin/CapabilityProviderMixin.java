package net.flameslight.tacz_noendman_tweaks.mixin;

import net.flameslight.tacz_noendman_tweaks.types.DataHolderCache;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears an entity's cached TACZ DataHolder capability whenever Forge invalidates or revives its
 * capabilities. While capabilities are invalid, Forge returns an empty result, and the next lookup then does
 * the full original lookup again, so the cache never returns a holder TACZ's own code would not.
 */
@Mixin(value = CapabilityProvider.class, remap = false)
public abstract class CapabilityProviderMixin {

    /**
     * Clears the cache before the capabilities are invalidated.
     *
     * @param ci the callback info
     */
    @Inject(method = "invalidateCaps", at = @At("HEAD"))
    private void tacz_noendman$clearOnInvalidate(CallbackInfo ci) {
        tacz_noendman$clear();
    }

    /**
     * Clears the cache before the capabilities are revived.
     *
     * @param ci the callback info
     */
    @Inject(method = "reviveCaps", at = @At("HEAD"))
    private void tacz_noendman$clearOnRevive(CallbackInfo ci) {
        tacz_noendman$clear();
    }

    /**
     * Clears the cached holder when this provider is an entity.
     */
    private void tacz_noendman$clear() {
        if ((Object) this instanceof DataHolderCache cache) {
            cache.tacz_noendman$setCachedHolder(null);
        }
    }
}

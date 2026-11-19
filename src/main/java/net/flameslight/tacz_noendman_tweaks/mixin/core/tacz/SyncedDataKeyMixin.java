package net.flameslight.tacz_noendman_tweaks.mixin.core.tacz;

import com.tacz.guns.entity.sync.core.SyncedDataKey;
import net.flameslight.tacz_noendman_tweaks.types.RegisteredKeyFlag;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two additions to TACZ's SyncedDataKey.
 * <ul>
 *     <li>Fix 1: hashCode returns a cached copy of pairKey.hashCode(). TACZ builds pairKey with Pair.of, an
 *     ImmutablePair of two ResourceLocations, and the field is final, so the value never changes.</li>
 *     <li>Fix 2: the "registered" mark. It is set only by SyncedEntityDataMixin, right after TACZ registered
 *     the key.</li>
 * </ul>
 */
@Mixin(value = SyncedDataKey.class, remap = false)
public abstract class SyncedDataKeyMixin implements RegisteredKeyFlag {

    @Shadow
    @Final
    private Pair<ResourceLocation, ResourceLocation> pairKey;

    @Unique
    private boolean tacz_noendman$registered;

    /*
        Cached hash, the same scheme java.lang.String uses: 0 means "not computed yet" unless hashIsZero is set.
        Safe without locks: a thread that sees 0 just computes the same value again.
     */
    @Unique
    private int tacz_noendman$hash;

    @Unique
    private boolean tacz_noendman$hashIsZero;

    /**
     * Returns the cached hash code, computing it from pairKey on first use like TACZ's own hashCode.
     *
     * @param cir the callback, set to the hash code
     */
    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void tacz_noendman$cachedHashCode(CallbackInfoReturnable<Integer> cir) {
        int hash = this.tacz_noendman$hash;
        if (hash == 0 && !this.tacz_noendman$hashIsZero) {
            hash = this.pairKey.hashCode();
            if (hash == 0) {
                this.tacz_noendman$hashIsZero = true;
            } else {
                this.tacz_noendman$hash = hash;
            }
        }
        cir.setReturnValue(hash);
    }

    /**
     * Tells whether this exact key object was registered.
     *
     * @return true after TACZ's registerDataKey accepted this key
     */
    @Override
    public boolean tacz_noendman$isRegistered() {
        return this.tacz_noendman$registered;
    }

    /**
     * Marks this key object as registered.
     */
    @Override
    public void tacz_noendman$markRegistered() {
        this.tacz_noendman$registered = true;
    }
}

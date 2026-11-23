package net.flameslight.tacz_noendman_tweaks.mixin.tacz;

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
 * Two additions to TaCZ's SyncedDataKey to reduce its runtime operations. TaCZ looks a key up in hash sets and maps
 * on every read and write of an entity's synced data (aiming, reloading and so on), constantly and for every entity.
 * The additions keep the same results as TaCZ's own code, so there is no gameplay impact:
 * <ul>
 *     <li>HashCode is computed once and cached. TaCZ recomputes it from pairKey (an ImmutablePair of two
 *     ResourceLocations) on every call, although the field is final and the value never changes.</li>
 *     <li>A "registered" mark, set only by SyncedEntityDataMixin right after TaCZ registered the key, so
 *     SyncedEntityDataMixin can skip TaCZ's registration lookup for it.</li>
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
     * Returns the cached hash code, computing it from pairKey on first use like TaCZ's own hashCode.
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
     * @return true after TaCZ's registerDataKey accepted this key
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

package net.flameslight.tacz_noendman_tweaks.mixin.tacz;

import com.tacz.guns.entity.sync.core.DataEntry;
import com.tacz.guns.entity.sync.core.DataHolder;
import com.tacz.guns.entity.sync.core.SyncedDataKey;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TaCZ's gatherDirty streams the data map through two filters into a new list. This builds the same
 * ArrayList, with the same entries in the same order, using a plain loop.
 */
@Mixin(value = DataHolder.class, remap = false)
public abstract class DataHolderMixin {

    @Shadow
    public Map<SyncedDataKey<?, ?>, DataEntry<?, ?>> dataMap;

    /**
     * Collects the dirty entries whose key is synced, like TaCZ's stream does.
     *
     * @param cir the callback, set to the collected list
     */
    @Inject(method = "gatherDirty", at = @At("HEAD"), cancellable = true)
    private void tacz_noendman$gatherDirtyLoop(CallbackInfoReturnable<List<DataEntry<?, ?>>> cir) {
        List<DataEntry<?, ?>> dirty = new ArrayList<>();
        for (DataEntry<?, ?> entry : this.dataMap.values()) {
            if (entry.isDirty() && entry.getKey().syncMode() != SyncedDataKey.SyncMode.NONE) {
                dirty.add(entry);
            }
        }
        ModLogger.debugOnce("fix active: dirty entry gathering");
        cir.setReturnValue(dirty);
    }
}

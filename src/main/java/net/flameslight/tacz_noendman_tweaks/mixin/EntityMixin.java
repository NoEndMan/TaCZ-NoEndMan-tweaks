package net.flameslight.tacz_noendman_tweaks.mixin;

import com.tacz.guns.entity.sync.core.DataHolder;
import net.flameslight.tacz_noendman_tweaks.types.DataHolderCache;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Gives every entity a slot for the cached TaCZ DataHolder capability result, so SyncedEntityDataMixin's getDataHolder
 * skips the scan over all attached capabilities.
 */
@Mixin(Entity.class)
public abstract class EntityMixin implements DataHolderCache {

    @Unique
    private LazyOptional<DataHolder> tacz_noendman$cachedHolder;

    /**
     * Returns the cached capability result.
     *
     * @return the cached LazyOptional, or null when nothing is cached
     */
    @Override
    public LazyOptional<DataHolder> tacz_noendman$getCachedHolder() {
        return this.tacz_noendman$cachedHolder;
    }

    /**
     * Stores or clears the cached capability result.
     *
     * @param holder the LazyOptional to cache, or null to clear the cache
     */
    @Override
    public void tacz_noendman$setCachedHolder(LazyOptional<DataHolder> holder) {
        this.tacz_noendman$cachedHolder = holder;
    }
}

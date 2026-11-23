package net.flameslight.tacz_noendman_tweaks.types;

import com.tacz.guns.entity.sync.core.DataHolder;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Added to every Entity by a mixin. Holds the LazyOptional that TaCZ's DataHolder capability lookup returned,
 * so later lookups skip the scan over all attached capabilities.
 */
public interface DataHolderCache {

    /**
     * Returns the cached capability result.
     *
     * @return the cached LazyOptional, or null when nothing is cached
     */
    LazyOptional<DataHolder> tacz_noendman$getCachedHolder();

    /**
     * Stores or clears the cached capability result.
     *
     * @param holder the LazyOptional to cache, or null to clear the cache
     */
    void tacz_noendman$setCachedHolder(LazyOptional<DataHolder> holder);
}

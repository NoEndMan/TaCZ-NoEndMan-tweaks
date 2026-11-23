package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import net.minecraftforge.fml.ModList;

/**
 * TaCZ compatibility: whether it is installed. TaCZ own classes are only used in the TaCZ mixins (applied only with
 * TaCZ, see TaCZMixinPlugin), in TaCZLayers, TaCZFirstEquipWarmUp and TaCZFirstEquipPreRender.
 */
public final class TaCZCompat {

    /** Mod id of TaCZ. */
    public static final String MOD_ID = "tacz";

    /** True when TaCZ is installed. */
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);
}

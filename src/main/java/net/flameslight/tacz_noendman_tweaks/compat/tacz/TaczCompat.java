package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import net.minecraftforge.fml.ModList;

/**
 * TACZ compatibility: whether it is installed. TACZ own classes are only used in the TACZ mixins (applied only with
 * TACZ, see TaczMixinPlugin), in TaczLayers and in TaczFirstEquipWarmUp.
 */
public final class TaczCompat {

    /** Mod id of TACZ. */
    public static final String MOD_ID = "tacz";

    /** True when TACZ is installed. */
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);
}

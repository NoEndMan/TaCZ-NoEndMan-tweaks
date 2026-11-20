package net.flameslight.tacz_noendman_tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/**
 * The mod's common config, with cached values.
 */
public class CommonConfig {
    /** Default of isDebugMod. */
    public static final boolean DEFAULT_DEBUG_MOD = false;

    /** The registered common config spec. */
    public static final ForgeConfigSpec SPEC;
    /** isDebugMod option. */
    public static final ForgeConfigSpec.BooleanValue DEBUG_MOD;

    private static volatile boolean resolved = false;
    private static volatile boolean isDebugMod = DEFAULT_DEBUG_MOD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("common mod configuration.").push("general");

        DEBUG_MOD = builder
                .comment("Enables this mod's own [DEBUG] log lines.")
                .define("isDebugMod", DEFAULT_DEBUG_MOD);

        builder.pop();
        SPEC = builder.build();
    }

    /**
     * Drops the cached values so they are read again.
     */
    public static void refresh() {
        resolved = false;
    }

    /**
     * Reads every option into the cache.
     *
     * @return false while the config is not loaded yet
     */
    private static boolean resolve() {
        if (resolved)
            return true;

        if (!SPEC.isLoaded())
            return false;

        isDebugMod = DEBUG_MOD.get();
        resolved = true;
        return true;
    }

    /**
     * Reads isDebugMod. Returns the default until the config has loaded.
     *
     * @return the option's value
     */
    public static boolean isDebugMod() {
        return resolve() ? isDebugMod : DEFAULT_DEBUG_MOD;
    }

    /**
     * Refreshes the config cache when this config loads or reloads.
     *
     * @param config    the config of the event
     * @param unloading true if the config is unloading
     */
    public static void onConfigLoaded(ModConfig config, boolean unloading) {
        if (!unloading && config.getSpec() == SPEC)
            refresh();
    }
}

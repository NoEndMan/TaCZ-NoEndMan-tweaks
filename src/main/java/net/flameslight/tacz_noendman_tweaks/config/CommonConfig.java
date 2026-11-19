package net.flameslight.tacz_noendman_tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/**
 * The mod's common config (config/tacz_noendman_tweaks-common.toml). Its only option is isDebugMod.
 */
public class CommonConfig {
    /** Default of isDebugMod. */
    public static final boolean DEFAULT_DEBUG_MOD = false;

    /** The registered common config spec. */
    public static final ForgeConfigSpec CLIENT_SPEC;
    /** isDebugMod option. */
    public static final ForgeConfigSpec.BooleanValue DEBUG_MOD;

    private static volatile boolean isDebugMod = DEFAULT_DEBUG_MOD;
    private static volatile boolean isDebugModResolved = false;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("common mod configuration.").push("general");

        DEBUG_MOD = builder
                .comment("Enables this mod's own [DEBUG] log lines.")
                .define("isDebugMod", DEFAULT_DEBUG_MOD);

        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    /**
     * Drops the cached option value, so the next read takes it from the config again. Called on config load and reload.
     */
    public static void refresh() {
        isDebugModResolved = false;
    }

    /**
     * Reads isDebugMod.
     * <p>
     * Safe at any point in the lifecycle: returns the default until the config file has actually
     * been loaded, rather than throwing the way ConfigValue.get() does.
     *
     * @return the option's value, the default until the config file has loaded
     */
    public static boolean isDebugMod() {
        if (!isDebugModResolved) {
            if (!CLIENT_SPEC.isLoaded()) {
                return DEFAULT_DEBUG_MOD;
            }
            isDebugMod = DEBUG_MOD.get();
            isDebugModResolved = true;
        }

        return isDebugMod;
    }

    /**
     * Refreshes the config cache when this mod's config loads or reloads.
     *
     * @param config    the config of the event
     * @param unloading true if the config is unloading
     */
    public static void onConfigLoaded(ModConfig config, boolean unloading) {
        if (unloading || config.getSpec() != CLIENT_SPEC) {
            return;
        }
        refresh();
    }
}

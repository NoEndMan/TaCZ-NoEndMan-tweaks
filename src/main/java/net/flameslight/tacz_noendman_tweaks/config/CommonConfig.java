package net.flameslight.tacz_noendman_tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The mod's common config.
 */
public class CommonConfig {
    /** Default of isDebugMod. */
    public static final boolean DEFAULT_DEBUG_MOD = false;

    /** The registered common config spec. */
    public static final ForgeConfigSpec SPEC;
    /** isDebugMod option. */
    public static final ForgeConfigSpec.BooleanValue DEBUG_MOD;

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
     * Reads isDebugMod. Returns the default until the config has loaded.
     *
     * @return the option's value
     */
    public static boolean isDebugMod() {
        return SPEC.isLoaded() ? DEBUG_MOD.get() : DEFAULT_DEBUG_MOD;
    }
}

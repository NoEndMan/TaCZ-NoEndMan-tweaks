package net.flameslight.tacz_noendman_tweaks;

import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Entry point of TaCZ NoEndMan Tweaks. Registers the configs. Event listeners live in the events package,
 * and the TaCZ fixes are mixins.
 */
@Mod(TaCZNoEndManTweaks.MOD_ID)
public final class TaCZNoEndManTweaks {

    /** The mod id. */
    public static final String MOD_ID = "tacz_noendman_tweaks";

    /**
     * Creates the mod and registers its configs, in a config folder named after the mod id.
     *
     * @param context the mod loading context Forge passes in
     */
    public TaCZNoEndManTweaks(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, MOD_ID + "/common.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "/client.toml");
    }
}

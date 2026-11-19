package net.flameslight.tacz_noendman_tweaks;

import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Entry point of [Tacz] NoEndMan Tweaks. Registers the config. Event listeners live in the events package,
 * and the TACZ fixes are mixins.
 */
@Mod(TaczNoEndManTweaks.MOD_ID)
public final class TaczNoEndManTweaks {

    /** The mod id. */
    public static final String MOD_ID = "tacz_noendman_tweaks";

    /** Mod id of Immersive Aircraft. */
    public static final String IMMERSIVE_AIRCRAFT_ID = "immersive_aircraft";

    /** Mod id of Entity Model Features. */
    public static final String EMF_ID = "entity_model_features";

    /** Mod id of Player Animator. */
    public static final String PLAYER_ANIMATOR_ID = "playeranimator";

    /**
     * Creates the mod and registers its config.
     *
     * @param context the mod loading context Forge passes in
     */
    public TaczNoEndManTweaks(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.CLIENT_SPEC);
    }
}

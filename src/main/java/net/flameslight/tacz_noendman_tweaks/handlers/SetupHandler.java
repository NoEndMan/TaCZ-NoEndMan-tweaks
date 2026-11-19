package net.flameslight.tacz_noendman_tweaks.handlers;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraftforge.fml.ModList;

/**
 * Common setup work.
 */
public class SetupHandler {
    /**
     * Logs which optional integrations are present, as a [DEBUG] line.
     */
    public static void logIntegrations() {
        ModList mods = ModList.get();
        ModLogger.debug("Integrations: Immersive Aircraft {}, Entity Model Features {}, Player Animator {}",
                mods.isLoaded(TaczNoEndManTweaks.IMMERSIVE_AIRCRAFT_ID), mods.isLoaded(TaczNoEndManTweaks.EMF_ID),
                mods.isLoaded(TaczNoEndManTweaks.PLAYER_ANIMATOR_ID));
    }
}

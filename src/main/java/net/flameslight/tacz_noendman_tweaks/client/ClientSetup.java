package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfTwistHook;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.RidingGunAimLayer;
import net.minecraftforge.fml.ModList;

/**
 * Client setup work. Hooks the upper body twist into Entity Model Features when both Immersive Aircraft and
 * EMF are installed, and adds the riding gun aim layer when Player Animator is installed. Called by ModEvents
 * during client setup.
 */
public final class ClientSetup {

    /**
     * Registers the EMF animation hook and the Player Animator layer when needed. Runs on the main thread during
     * client setup.
     */
    public static void init() {
        ModList mods = ModList.get();
        if (mods.isLoaded(TaczNoEndManTweaks.IMMERSIVE_AIRCRAFT_ID) && mods.isLoaded(TaczNoEndManTweaks.EMF_ID)) {
            EmfTwistHook.register();
        }
        if (mods.isLoaded(TaczNoEndManTweaks.PLAYER_ANIMATOR_ID)) {
            RidingGunAimLayer.register();
        }
    }
}

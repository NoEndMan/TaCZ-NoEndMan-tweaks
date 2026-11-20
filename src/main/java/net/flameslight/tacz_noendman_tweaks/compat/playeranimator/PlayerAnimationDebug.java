package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;

/**
 * Debug logging of Player Animator layers. Only reached when Player Animator is installed.
 */
public final class PlayerAnimationDebug {

    /*
        The local player's active layers at the last check, for the debug log.
     */
    private static String lastActiveLayers = "";

    /**
     * Logs the local player's active layers in debug mode when they change.
     *
     * @param active the active layers, empty when none
     */
    static void logActiveLayers(String active) {
        if (!active.equals(lastActiveLayers)) {
            lastActiveLayers = active;
            ModLogger.debug("non-TACZ Player Animator layers active on the local player: {}",
                    active.isEmpty() ? "none" : active);
        }
    }

    /**
     * Describes a layer for the debug log.
     *
     * @param animation the layer
     * @return the description
     */
    static String describe(IAnimation animation) {
        String text = animation.getClass().getName();
        if (animation instanceof ModifierLayer<?> modifier && modifier.getAnimation() != null)
            text += " playing " + modifier.getAnimation().getClass().getName();

        if (animation instanceof OutsideVehicleGunTwistLayer)
            text += " (this mod)";

        return text;
    }
}

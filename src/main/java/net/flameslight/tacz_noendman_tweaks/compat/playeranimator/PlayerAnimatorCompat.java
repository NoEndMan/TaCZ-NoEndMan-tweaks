package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OnVehicleGunAimLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Player Animator compatibility: whether it is installed, and safe access to it. Player Animator
 * own classes are only used in PlayerAnimatorAccessor and the layers.
 */
public final class PlayerAnimatorCompat {

    /** Mod id of Player Animator. */
    public static final String MOD_ID = "playeranimator";

    /** True when Player Animator is installed. */
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    /**
     * Registers this mod's Player Animator layers, when Player Animator is installed.
     */
    public static void registerLayers() {
        if (LOADED) {
            OnVehicleGunAimLayer.register();
            OutsideVehicleGunTwistLayer.register();
        }
    }

    /**
     * Tells whether TACZ animates the player's gun holding pose.
     *
     * @param player the player
     * @return true while a gun pose plays, false without Player Animator
     */
    public static boolean isTaczGunPose(AbstractClientPlayer player) {
        return LOADED && PlayerAnimatorAccessor.isTaczGunPose(player);
    }

    /**
     * Tells whether another mod's Player Animator animation plays on the player.
     *
     * @param player the player
     * @return true when another mod's animation is active, false without Player Animator
     */
    public static boolean isAnimating(AbstractClientPlayer player) {
        return LOADED && PlayerAnimatorAccessor.isAnimating(player);
    }

    /**
     * Describes which gun layers are active, for the debug log.
     *
     * @param player the player
     * @return the state of each layer
     */
    public static String describeGunLayers(AbstractClientPlayer player) {
        return LOADED
                ? PlayerAnimationDebug.describeGunLayers(player)
                : "no Player Animator";
    }

    /**
     * Describes the whole player rotation Player Animator gives this frame, for the debug log.
     *
     * @param player    the player
     * @param tickDelta the partial tick
     * @return the rotation in degrees
     */
    public static String describeWholeBodyRotation(AbstractClientPlayer player, float tickDelta) {
        return LOADED
                ? PlayerAnimationDebug.describeWholeBodyRotation(player, tickDelta)
                : "no Player Animator";
    }
}

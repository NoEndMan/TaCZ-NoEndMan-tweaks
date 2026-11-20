package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
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
        if (LOADED)
            OutsideVehicleGunTwistLayer.register();
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
}

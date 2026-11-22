package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.resources.ResourceLocation;

/**
 * Ids of TACZ's Player Animator layers. Only call it when TaczCompat.LOADED is true.
 */
public final class TaczLayers {

    /**
     * Returns the id of TACZ's lower body layer.
     *
     * @return the id, may be null before TACZ registered it
     */
    public static ResourceLocation lower() {
        return PlayerAnimatorCompat.LOWER_ANIMATION;
    }

    /**
     * Returns the id of TACZ's looping upper body layer.
     *
     * @return the id, may be null before TACZ registered it
     */
    public static ResourceLocation loopUpper() {
        return PlayerAnimatorCompat.LOOP_UPPER_ANIMATION;
    }

    /**
     * Returns the id of TACZ's one-shot upper body layer.
     *
     * @return the id, may be null before TACZ registered it
     */
    public static ResourceLocation onceUpper() {
        return PlayerAnimatorCompat.ONCE_UPPER_ANIMATION;
    }

    /**
     * Returns the id of TACZ's gun pose rotation layer.
     *
     * @return the id, may be null before TACZ registered it
     */
    public static ResourceLocation rotation() {
        return PlayerAnimatorCompat.ROTATION_ANIMATION;
    }
}

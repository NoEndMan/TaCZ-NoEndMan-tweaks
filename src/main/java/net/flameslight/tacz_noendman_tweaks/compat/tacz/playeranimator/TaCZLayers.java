package net.flameslight.tacz_noendman_tweaks.compat.tacz.playeranimator;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.resources.ResourceLocation;

/**
 * Ids of TaCZ's Player Animator layers. Only call it when TaCZCompat.LOADED is true.
 */
public final class TaCZLayers {

    /**
     * Returns the id of TaCZ's lower body layer.
     *
     * @return the id, may be null before TaCZ registered it
     */
    public static ResourceLocation lower() {
        return PlayerAnimatorCompat.LOWER_ANIMATION;
    }

    /**
     * Returns the id of TaCZ's looping upper body layer.
     *
     * @return the id, may be null before TaCZ registered it
     */
    public static ResourceLocation loopUpper() {
        return PlayerAnimatorCompat.LOOP_UPPER_ANIMATION;
    }

    /**
     * Returns the id of TaCZ's one-shot upper body layer.
     *
     * @return the id, may be null before TaCZ registered it
     */
    public static ResourceLocation onceUpper() {
        return PlayerAnimatorCompat.ONCE_UPPER_ANIMATION;
    }

    /**
     * Returns the id of TaCZ's gun pose rotation layer.
     *
     * @return the id, may be null before TaCZ registered it
     */
    public static ResourceLocation rotation() {
        return PlayerAnimatorCompat.ROTATION_ANIMATION;
    }
}

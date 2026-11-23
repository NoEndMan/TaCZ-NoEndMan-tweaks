package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
import net.flameslight.tacz_noendman_tweaks.compat.tacz.TaCZCompat;
import net.flameslight.tacz_noendman_tweaks.compat.tacz.playeranimator.TaCZLayers;
import net.flameslight.tacz_noendman_tweaks.util.ReflectionUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The Player Animator code behind PlayerAnimatorCompat. Only reached when Player Animator is installed.
 */
public final class PlayerAnimatorAccessor {

    /**
     * Tells whether another mod's Player Animator animation plays on the player. TaCZ's and this mod's layers are
     * ignored.
     *
     * @param player the player
     * @return true when another mod's animation is active
     */
    static boolean isAnimating(AbstractClientPlayer player) {
        AnimationStack stack;

        try {
            stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!stack.isActive())
            return false;

        // When the private layer list can't be read, any active stack counts as animating.
        Field layers = ReflectionUtils.getAnimationStackLayersField();

        if (layers == null)
            return true;

        PlayerAnimationAccess.PlayerAssociatedAnimationData data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        boolean tacz = TaCZCompat.LOADED;
        IAnimation lower = tacz ? taczLayer(data, TaCZLayers.lower()) : null;
        IAnimation loopUpper = tacz ? taczLayer(data, TaCZLayers.loopUpper()) : null;
        IAnimation onceUpper = tacz ? taczLayer(data, TaCZLayers.onceUpper()) : null;
        IAnimation rotation = tacz ? taczLayer(data, TaCZLayers.rotation()) : null;

        try {
            for (Object entry : (List<?>) layers.get(stack)) {
                Pair<?, ?> pair = (Pair<?, ?>) entry;
                IAnimation animation = (IAnimation) pair.getRight();

                if (animation == lower || animation == loopUpper || animation == onceUpper || animation == rotation
                        || animation instanceof OutsideVehicleGunTwistLayer) {
                    continue;
                }

                if (animation.isActive())
                    return true;
            }
        } catch (IllegalAccessException | ClassCastException e) {
            return true;
        }

        return false;
    }

    /**
     * Tells whether TaCZ animates the player's gun holding pose.
     *
     * @param player the player
     * @return true while a gun pose plays
     */
    static boolean isTaczGunPose(AbstractClientPlayer player) {
        try {
            IAnimation rotation = taczLayer(PlayerAnimationAccess.getPlayerAssociatedData(player), TaCZLayers.rotation());
            return rotation != null && rotation.isActive();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns one of TaCZ's registered animation layers for the player.
     *
     * @param data the player's associated animation data
     * @param id   the layer id, may be null before TaCZ registered it
     * @return the layer, or null when there is none
     */
    static IAnimation taczLayer(PlayerAnimationAccess.PlayerAssociatedAnimationData data, ResourceLocation id) {
        return id == null ? null : data.get(id);
    }
}

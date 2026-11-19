package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.flameslight.tacz_noendman_tweaks.util.ReflectionUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Player Animator access. Only loaded when Player Animator is installed.
 */
public final class PlayerAnimatorCheck {

    /**
     * Tells whether a Player Animator animation from another mod is playing on the player. TACZ's own gun layers
     * (held, aiming, reloading and rotation poses) are ignored: they play the whole time a gun is held, and the
     * twist is added on top of them anyway.
     *
     * @param player the player
     * @return true when a non-TACZ layer of the player's animation stack is active
     */
    public static boolean isAnimating(AbstractClientPlayer player) {
        AnimationStack stack;
        try {
            stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!stack.isActive()) {
            return false;
        }
        /*
            AnimationStack keeps its layers in a private field. When it can't be read, any active stack counts as
            animating, like before.
         */
        Field layers = ReflectionUtils.getAnimationStackLayersField();
        if (layers == null) {
            return true;
        }
        PlayerAnimationAccess.PlayerAssociatedAnimationData data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        IAnimation lower = taczLayer(data, PlayerAnimatorCompat.LOWER_ANIMATION);
        IAnimation loopUpper = taczLayer(data, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION);
        IAnimation onceUpper = taczLayer(data, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
        IAnimation rotation = taczLayer(data, PlayerAnimatorCompat.ROTATION_ANIMATION);
        try {
            for (Object entry : (List<?>) layers.get(stack)) {
                IAnimation animation = (IAnimation) ((Pair<?, ?>) entry).getRight();
                if (animation == lower || animation == loopUpper || animation == onceUpper || animation == rotation) {
                    continue;
                }
                if (animation.isActive()) {
                    return true;
                }
            }
            return false;
        } catch (IllegalAccessException | ClassCastException e) {
            return true;
        }
    }

    /**
     * Tells whether TACZ animates the player's gun holding pose. TACZ plays its rotation layer the whole time a gun
     * with a third person animation is held. That layer gives the head and arms pitch only, and turns the whole
     * player toward the camera instead, except while riding, when it skips that turn.
     *
     * @param player the player
     * @return true when TACZ's rotation layer is active on the player
     */
    public static boolean isTaczGunPose(AbstractClientPlayer player) {
        try {
            IAnimation rotation = taczLayer(PlayerAnimationAccess.getPlayerAssociatedData(player), PlayerAnimatorCompat.ROTATION_ANIMATION);
            return rotation != null && rotation.isActive();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns one of TACZ's registered animation layers for the player.
     *
     * @param data the player's associated animation data
     * @param id   the layer id, may be null before TACZ registered it
     * @return the layer, or null when there is none
     */
    private static IAnimation taczLayer(PlayerAnimationAccess.PlayerAssociatedAnimationData data, ResourceLocation id) {
        return id == null ? null : data.get(id);
    }
}

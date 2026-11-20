package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.util.ReflectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The Player Animator code behind PlayerAnimatorCompat. Only reached when Player Animator is installed.
 */
public final class PlayerAnimatorAccessor {

    /**
     * Tells whether another mod's Player Animator animation plays on the player. TACZ's and this mod's layers are
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
        IAnimation lower = taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.LOWER_ANIMATION);
        IAnimation loopUpper = taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.LOOP_UPPER_ANIMATION);
        IAnimation onceUpper = taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
        IAnimation rotation = taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.ROTATION_ANIMATION);
        boolean debug = CommonConfig.isDebugMod() && player == Minecraft.getInstance().player;
        StringBuilder active = debug ? new StringBuilder() : null;
        boolean animating = false;

        try {
            for (Object entry : (List<?>) layers.get(stack)) {
                Pair<?, ?> pair = (Pair<?, ?>) entry;
                IAnimation animation = (IAnimation) pair.getRight();

                if (animation == lower || animation == loopUpper || animation == onceUpper || animation == rotation
                        || animation instanceof OutsideVehicleGunTwistLayer) {
                    continue;
                }

                if (animation.isActive()) {
                    if (!debug)
                        return true;

                    animating = true;
                    active.append(active.isEmpty() ? "" : ", ").append("priority ").append(pair.getLeft())
                            .append(' ').append(PlayerAnimationDebug.describe(animation));
                }
            }
        } catch (IllegalAccessException | ClassCastException e) {
            return true;
        }

        if (debug)
            PlayerAnimationDebug.logActiveLayers(active.toString());

        return animating;
    }

    /**
     * Tells whether TACZ animates the player's gun holding pose.
     *
     * @param player the player
     * @return true while a gun pose plays
     */
    static boolean isTaczGunPose(AbstractClientPlayer player) {
        try {
            IAnimation rotation = taczLayer(PlayerAnimationAccess.getPlayerAssociatedData(player), com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.ROTATION_ANIMATION);
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
    static IAnimation taczLayer(PlayerAnimationAccess.PlayerAssociatedAnimationData data, ResourceLocation id) {
        return id == null ? null : data.get(id);
    }
}

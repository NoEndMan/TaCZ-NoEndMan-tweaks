package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OnVehicleGunAimLayer;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.util.ReflectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The Player Animator code behind PlayerAnimatorCompat, and the upper body turn helpers of this mod's layers. Only
 * reached when Player Animator is installed.
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
                        || animation instanceof OnVehicleGunAimLayer || animation instanceof OutsideVehicleGunTwistLayer) {
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

    /**
     * Tells whether a part belongs to the upper body that turns.
     *
     * @param modelName the Player Animator part name
     * @return true for the torso, head and both arms
     */
    public static boolean isUpperBody(String modelName) {
        return "torso".equals(modelName) || "head".equals(modelName)
                || "rightArm".equals(modelName) || "leftArm".equals(modelName);
    }

    /**
     * Turns one upper body part value around the vertical axis.
     *
     * @param type  the transform type, POSITION or ROTATION
     * @param value the part's pivot in model units, or its rotation in radians
     * @param yaw   the turn in radians
     * @return the turned value
     */
    public static Vec3f turn(TransformType type, Vec3f value, float yaw) {
        return type == TransformType.POSITION ? turnPosition(value, yaw) : turnRotation(value, yaw);
    }

    /**
     * Turns one upper body part value, with the head turning only as far as the look angle.
     *
     * @param player    the player
     * @param modelName the Player Animator part name
     * @param type      the transform type, POSITION or ROTATION
     * @param value     the part's pivot in model units, or its rotation in radians
     * @param yaw       the upper body turn in radians
     * @param tickDelta the partial tick
     * @return the turned value
     */
    public static Vec3f turnPart(AbstractClientPlayer player, String modelName, TransformType type, Vec3f value,
                                 float yaw, float tickDelta) {
        if (type == TransformType.ROTATION && "head".equals(modelName)) {
            float target = TurnMathUtils.headTargetDegrees(player, tickDelta) * Mth.DEG_TO_RAD;
            return turnRotation(value, TurnMathUtils.headTurn(value.getY(), target, yaw));
        }
        return turn(type, value, yaw);
    }

    /**
     * Moves a part pivot around the vertical axis through the model origin.
     *
     * @param position the pivot in model units
     * @param yaw      the turn in radians
     * @return the moved pivot
     */
    private static Vec3f turnPosition(Vec3f position, float yaw) {
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        float x = position.getX();
        float z = position.getZ();
        return new Vec3f(x * cos + z * sin, position.getY(), -x * sin + z * cos);
    }

    /**
     * Turns a part rotation around the vertical axis.
     *
     * @param rotation the rotation in radians, as x, y and z angles
     * @param yaw      the turn in radians
     * @return the turned rotation
     */
    private static Vec3f turnRotation(Vec3f rotation, float yaw) {
        float[] turned = TurnMathUtils.turnRotation(rotation.getX(), rotation.getY(), rotation.getZ(), yaw);
        return new Vec3f(turned[0], turned[1], turned[2]);
    }
}

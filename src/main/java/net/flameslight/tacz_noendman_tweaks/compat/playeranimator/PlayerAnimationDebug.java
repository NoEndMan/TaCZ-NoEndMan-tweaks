package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.flameslight.tacz_noendman_tweaks.logger.debug.PoseDebug;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OutsideVehicleGunTwistLayer;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers.OnVehicleGunAimLayer;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

/**
 * Debug logging of Player Animator layers and of this mod's layer turns. Only reached when Player Animator is
 * installed.
 */
public final class PlayerAnimationDebug {

    /*
        The local player's active layers at the last check, for the debug log.
     */
    private static String lastActiveLayers = "";

    /**
     * Logs a layer's turn state in debug mode.
     *
     * @param layer     the layer name for the log
     * @param player    the player
     * @param tickDelta the partial tick
     * @param turn      the upper body turn in degrees
     * @param bodyTurn  the whole body turn in degrees that is kept
     */
    public static void logState(String layer, AbstractClientPlayer player, float tickDelta, float turn, float bodyTurn) {
        if (!PoseDebug.shouldLog(player, layer + " state"))
            return;

        Entity vehicle = player.getVehicle();
        ModLogger.debug("{}: vehicle {}, net head yaw {}, body yaw {}, head yaw {}, pitch {}, upper body turn {}, whole body turn {}",
                layer, vehicle == null ? "none" : vehicle.getClass().getName(), TurnMathUtils.headYaw(player, tickDelta),
                Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot),
                Mth.rotLerp(tickDelta, player.yHeadRotO, player.yHeadRot),
                Mth.lerp(tickDelta, player.xRotO, player.getXRot()), turn, bodyTurn);
    }

    /**
     * Logs one part's change in debug mode.
     *
     * @param layer     the layer name for the log
     * @param player    the player
     * @param modelName the Player Animator part name
     * @param type      the transform type
     * @param yaw       the upper body turn in radians, used for the rotation check
     * @param in        the value the lower layers produced
     * @param out       the changed value
     */
    public static void logTransform(String layer, AbstractClientPlayer player, String modelName, TransformType type,
                                    float yaw, Vec3f in, Vec3f out) {
        if (!PoseDebug.shouldLog(player, layer + " " + modelName + " " + type))
            return;

        if (type == TransformType.POSITION) {
            ModLogger.debug("{}: {} position in {}, out {}", layer, modelName,
                    PoseDebug.offsets(in.getX(), in.getY(), in.getZ()), PoseDebug.offsets(out.getX(), out.getY(), out.getZ()));
            return;
        }

        if (!PlayerAnimatorAccessor.isUpperBody(modelName)) {
            ModLogger.debug("{}: {} rotation in {}, out {}", layer, modelName,
                    PoseDebug.angles(in.getX(), in.getY(), in.getZ()), PoseDebug.angles(out.getX(), out.getY(), out.getZ()));
            return;
        }

        Quaternionf expected = new Quaternionf().rotationY(yaw).mul(new Quaternionf().rotationZYX(in.getZ(), in.getY(), in.getX()));
        Quaternionf written = new Quaternionf().rotationZYX(out.getZ(), out.getY(), out.getX());
        float error = 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(expected.dot(written)))) * Mth.RAD_TO_DEG;

        ModLogger.debug("{}: {} rotation in {}, out {}, conversion error {} degrees", layer, modelName,
                PoseDebug.angles(in.getX(), in.getY(), in.getZ()), PoseDebug.angles(out.getX(), out.getY(), out.getZ()), error);
    }

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

        if (animation instanceof OnVehicleGunAimLayer || animation instanceof OutsideVehicleGunTwistLayer)
            text += " (this mod)";

        return text;
    }

    /**
     * Describes which gun layers are active, for the debug log.
     *
     * @param player the player
     * @return the state of each layer
     */
    static String describeGunLayers(AbstractClientPlayer player) {
        PlayerAnimationAccess.PlayerAssociatedAnimationData data;

        try {
            data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        } catch (IllegalArgumentException e) {
            return "no animation data";
        }

        return "TACZ lower " + state(PlayerAnimatorAccessor.taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.LOWER_ANIMATION))
                + " loop " + state(PlayerAnimatorAccessor.taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.LOOP_UPPER_ANIMATION))
                + " once " + state(PlayerAnimatorAccessor.taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.ONCE_UPPER_ANIMATION))
                + " rotation " + state(PlayerAnimatorAccessor.taczLayer(data, com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.ROTATION_ANIMATION))
                + ", this mod riding " + state(data.get(OnVehicleGunAimLayer.ID))
                + " twist " + state(data.get(OutsideVehicleGunTwistLayer.ID));
    }

    /**
     * Describes the whole player rotation Player Animator gives this frame, for the debug log.
     *
     * @param player    the player
     * @param tickDelta the partial tick
     * @return the rotation in degrees
     */
    static String describeWholeBodyRotation(AbstractClientPlayer player, float tickDelta) {
        try {
            Vec3f rotation = PlayerAnimationAccess.getPlayerAnimLayer(player)
                    .get3DTransform("body", TransformType.ROTATION, tickDelta, Vec3f.ZERO);

            return PoseDebug.angles(rotation.getX(), rotation.getY(), rotation.getZ());
        } catch (IllegalArgumentException e) {
            return "no animation stack";
        }
    }

    /**
     * Formats whether a layer is active.
     *
     * @param animation the layer, may be null
     * @return "on", "off" or "none"
     */
    private static String state(IAnimation animation) {
        return animation == null
                ? "none"
                : animation.isActive()
                    ? "on"
                    : "off";
    }
}

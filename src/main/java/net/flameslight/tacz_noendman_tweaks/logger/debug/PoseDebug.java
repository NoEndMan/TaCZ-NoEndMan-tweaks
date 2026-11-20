package net.flameslight.tacz_noendman_tweaks.logger.debug;

import net.flameslight.tacz_noendman_tweaks.client.NoGunTwist;
import net.flameslight.tacz_noendman_tweaks.client.TwistFade;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Debug logging of the local player's upper body pose. Debug mode only.
 */
public final class PoseDebug {

    /*
        Per log key: the player tick it was last written on.
     */
    private static final Map<String, Integer> LAST_TICK = new HashMap<>();

    /*
        Body part of the local player's model while a bow or crossbow pose is logged this frame, and the tick of it.
     */
    private static ModelPart bowBody;
    private static int bowTick;

    /**
     * Tells whether a log line with this key should be written now, at most once per second.
     *
     * @param player the player being animated or drawn
     * @param key    the log key
     * @return true when the line should be written
     */
    public static boolean shouldLog(Entity player, String key) {
        if (!CommonConfig.isDebugMod() || player != Minecraft.getInstance().player || player.tickCount % 20 != 0) {
            return false;
        }
        AbstractClientPlayer local = (AbstractClientPlayer) player;
        boolean gunPose = PlayerAnimatorCompat.isTaczGunPose(local);
        if (!gunPose && TwistFade.strength(local, 1.0F) <= 0.0F) {
            return false;
        }
        Integer last = LAST_TICK.put(key, player.tickCount);
        return last == null || last != player.tickCount;
    }

    /**
     * Tells whether an arm pose aims with the head (bow, crossbow).
     *
     * @param pose the arm pose
     * @return true for the bow and crossbow poses
     */
    private static boolean isAimingPose(HumanoidModel.ArmPose pose) {
        return pose == HumanoidModel.ArmPose.BOW_AND_ARROW || pose == HumanoidModel.ArmPose.CROSSBOW_CHARGE
                || pose == HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    /**
     * Logs the upper body pose at one render step while aiming a bow or crossbow.
     *
     * @param stage  the render step
     * @param player the player being drawn
     * @param model  the player's model
     */
    public static void logBow(String stage, AbstractClientPlayer player, PlayerModel<?> model) {
        if (!CommonConfig.isDebugMod() || player != Minecraft.getInstance().player) {
            return;
        }
        boolean aiming = isAimingPose(model.rightArmPose) || isAimingPose(model.leftArmPose);
        if (!aiming || player.tickCount % 5 != 0) {
            if ("after render".equals(stage)) {
                bowBody = null;
            }
            return;
        }
        if ("after setupAnim".equals(stage)) {
            bowBody = model.body;
            bowTick = player.tickCount;
        }
        Integer last = LAST_TICK.put("bow " + stage, player.tickCount);
        if (last == null || last != player.tickCount) {
            ModLogger.debug("bow pose {}: arm poses right {} left {}, riding {}, pitch {}, net head yaw {}, no-gun twist {} degrees (strength {})",
                    stage, model.rightArmPose, model.leftArmPose, player.getVehicle() != null, player.getXRot(),
                    TurnMathUtils.headYaw(player, 1.0F), NoGunTwist.twistDegrees(player, 1.0F), TwistFade.strength(player, 1.0F));
            logBowParts(stage, model.body, model.head, model.rightArm, model.leftArm);
        }
        if ("after render".equals(stage)) {
            bowBody = null;
        }
    }

    /**
     * Logs the body, head and arm poses at an EMF step while aiming.
     *
     * @param stage    the render step
     * @param body     the body part
     * @param head     the head part, may be null
     * @param rightArm the right arm part, may be null
     * @param leftArm  the left arm part, may be null
     */
    public static void logBowParts(String stage, ModelPart body, ModelPart head, ModelPart rightArm, ModelPart leftArm) {
        if (bowBody == null || body != bowBody) {
            return;
        }
        Integer last = LAST_TICK.put("bow parts " + stage, bowTick);
        if (last != null && last == bowTick) {
            return;
        }
        ModLogger.debug("bow pose {}: body {}, head {}, rightArm {}, leftArm {}", stage, part(body),
                head == null ? "none" : part(head), rightArm == null ? "none" : part(rightArm),
                leftArm == null ? "none" : part(leftArm));
    }

    /**
     * Logs the pose of the player model's upper body parts and their outer layers.
     *
     * @param stage  where in the render the pose is read
     * @param player the player being drawn
     * @param model  the player's model
     */
    public static void logModel(String stage, AbstractClientPlayer player, PlayerModel<?> model) {
        if (!shouldLog(player, "model " + stage)) {
            return;
        }
        ModLogger.debug("gun pose {}: model class {}, part class {}", stage, model.getClass().getName(),
                model.body.getClass().getName());
        ModLogger.debug("gun pose {}: body {}, jacket {}", stage, part(model.body), part(model.jacket));
        ModLogger.debug("gun pose {}: head {}, hat {}", stage, part(model.head), part(model.hat));
        ModLogger.debug("gun pose {}: rightArm {}, rightSleeve {}", stage, part(model.rightArm), part(model.rightSleeve));
        ModLogger.debug("gun pose {}: leftArm {}, leftSleeve {}", stage, part(model.leftArm), part(model.leftSleeve));
    }

    /**
     * Formats a part pose.
     *
     * @param part the part
     * @return the pivot in model units and the rotation in degrees
     */
    public static String part(ModelPart part) {
        return "pos " + offsets(part.x, part.y, part.z) + " rot " + angles(part.xRot, part.yRot, part.zRot)
                + (part.visible ? "" : " hidden");
    }

    /**
     * Formats a stored part pose.
     *
     * @param pose the pose
     * @return the pivot in model units and the rotation in degrees
     */
    public static String part(PartPose pose) {
        return "pos " + offsets(pose.x, pose.y, pose.z) + " rot " + angles(pose.xRot, pose.yRot, pose.zRot);
    }

    /**
     * Formats a pivot or offset.
     *
     * @param x the x value
     * @param y the y value
     * @param z the z value
     * @return the values with two decimals
     */
    public static String offsets(float x, float y, float z) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", x, y, z);
    }

    /**
     * Formats a rotation given in radians.
     *
     * @param x the x angle in radians
     * @param y the y angle in radians
     * @param z the z angle in radians
     * @return the angles in degrees with one decimal
     */
    public static String angles(float x, float y, float z) {
        return String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", x * Mth.RAD_TO_DEG, y * Mth.RAD_TO_DEG, z * Mth.RAD_TO_DEG);
    }
}

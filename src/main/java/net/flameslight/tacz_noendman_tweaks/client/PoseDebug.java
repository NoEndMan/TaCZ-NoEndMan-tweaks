package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCheck;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Debug logging of the riding gun pose, only when isDebugMod is on. Each log key is written at most once per
 * second, and only for the local player while riding with a gun TACZ animates. Render thread only.
 */
public final class PoseDebug {

    private static final boolean PLAYER_ANIMATOR_LOADED = ModList.get().isLoaded(TaczNoEndManTweaks.PLAYER_ANIMATOR_ID);

    /*
        Per log key: the player tick it was last written on.
     */
    private static final Map<String, Integer> LAST_TICK = new HashMap<>();

    /**
     * Tells whether a log line with this key should be written now: debug mode is on, the player is the local
     * player riding with a TACZ gun pose, this is the first tick of a second, and the key was not written this tick.
     *
     * @param player the player being animated or drawn
     * @param key    the log key
     * @return true when the line should be written
     */
    public static boolean shouldLog(Entity player, String key) {
        if (!CommonConfig.isDebugMod() || player != Minecraft.getInstance().player || player.getVehicle() == null
                || player.tickCount % 20 != 0) {
            return false;
        }
        if (!PLAYER_ANIMATOR_LOADED || !PlayerAnimatorCheck.isTaczGunPose((AbstractClientPlayer) player)) {
            return false;
        }
        Integer last = LAST_TICK.put(key, player.tickCount);
        return last == null || last != player.tickCount;
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
        ModLogger.debug("riding gun pose {}: model class {}, part class {}", stage, model.getClass().getName(),
                model.body.getClass().getName());
        ModLogger.debug("riding gun pose {}: body {}, jacket {}", stage, part(model.body), part(model.jacket));
        ModLogger.debug("riding gun pose {}: head {}, hat {}", stage, part(model.head), part(model.hat));
        ModLogger.debug("riding gun pose {}: rightArm {}, rightSleeve {}", stage, part(model.rightArm), part(model.rightSleeve));
        ModLogger.debug("riding gun pose {}: leftArm {}, leftSleeve {}", stage, part(model.leftArm), part(model.leftSleeve));
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

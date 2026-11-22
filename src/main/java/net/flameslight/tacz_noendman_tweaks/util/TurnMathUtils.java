package net.flameslight.tacz_noendman_tweaks.util;

import net.flameslight.tacz_noendman_tweaks.compat.tacz.TaczCompat;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.config.TurnSettings;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Math shared by every upper body turn
 */
public final class TurnMathUtils {

    /*
        Per entity: the whole player yaw GunTwistLayer left this frame, in radians.
     */
    private static final Map<Entity, Float> WHOLE_BODY_YAW = new WeakHashMap<>();

    /**
     * Records the whole player yaw GunTwistLayer left this frame, for the head turn.
     *
     * @param entity  the player
     * @param radians the whole player yaw in radians
     */
    public static void setWholeBodyYaw(Entity entity, float radians) {
        WHOLE_BODY_YAW.put(entity, radians);
    }

    /**
     * Returns the yaw the head should end with so it faces where the player looks.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the head yaw in degrees, positive toward the same side as vanilla's head yaw
     */
    public static float headTargetDegrees(LivingEntity entity, float partialTick) {
        float yaw = headYaw(entity, partialTick);

        if (entity.getVehicle() != null) {
            return yaw;
        }

        Float body = WHOLE_BODY_YAW.get(entity);
        return body == null
                ? yaw
                : yaw + body * Mth.RAD_TO_DEG;
    }

    /**
     * Returns how far to turn the head so it reaches its target yaw without turning it twice.
     *
     * @param current the head's current yaw in radians
     * @param target  the yaw the head should end with in radians
     * @param turn    the upper body turn in radians
     * @return the head rotation turn in radians, between 0 and turn
     */
    public static float headTurn(float current, float target, float turn) {
        float missing = wrap(target - current);
        return turn >= 0.0F
                ? Mth.clamp(missing, 0.0F, turn)
                : Mth.clamp(missing, turn, 0.0F);
    }

    /**
     * Returns the head yaw relative to the body, the same way vanilla's renderer computes it.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the angle in degrees, positive toward the same side as vanilla's head yaw
     */
    public static float headYaw(LivingEntity entity, float partialTick) {
        float body = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float head = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        Entity vehicle = entity.getVehicle();

        if (vehicle != null
                && vehicle.shouldRiderSit()
                && vehicle instanceof LivingEntity mount) {
            body = Mth.rotLerp(partialTick, mount.yBodyRotO, mount.yBodyRot);
            float clamped = Mth.clamp(Mth.wrapDegrees(head - body),
                    -TurnSettings.MOUNT_HEAD_MAX_DEGREES, TurnSettings.MOUNT_HEAD_MAX_DEGREES);
            body = head - clamped;

            if (clamped * clamped > 2500.0F) {
                body += clamped * 0.2F;
            }
        }
        float yaw = Mth.wrapDegrees(head - body);
        yaw -= excessPastLimit(entity, yaw);
        return LivingEntityRenderer.isEntityUpsideDown(entity) ? -yaw : yaw;
    }

    /**
     * Returns how far the whole player turns on a vehicle that is not a living mount, so the head stays within the
     * head turn limit from the legs.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the whole body turn in degrees, 0 when none, positive toward the same side as vanilla's head yaw
     */
    public static float vehicleBodyExcess(LivingEntity entity, float partialTick) {
        float body = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float head = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);

        return excessPastLimit(entity, Mth.wrapDegrees(head - body));
    }

    /**
     * Returns the part of a head-to-body angle past the head turn limit, on a vehicle that is not a living mount.
     *
     * @param entity the entity
     * @param yaw    the head-to-body angle in degrees
     * @return the angle past the limit in degrees, 0 when within it or not on such a vehicle
     */
    private static float excessPastLimit(LivingEntity entity, float yaw) {
        Entity vehicle = entity.getVehicle();

        if (vehicle == null
                || (vehicle.shouldRiderSit() && vehicle instanceof LivingEntity)
                || !ClientConfig.isOnVehicle())
            return 0.0F;

        return yaw - Mth.clamp(yaw, -TurnSettings.MOUNT_HEAD_MAX_DEGREES, TurnSettings.MOUNT_HEAD_MAX_DEGREES);
    }

    /**
     * Tells whether the gun turn applies to the entity right now: TACZ installed, enableTurnWithGunFix on, and the
     * twist allowed for its situation.
     *
     * @param entity the entity
     * @return true when the turn applies to the entity right now
     */
    public static boolean isGunTurnEnabled(Entity entity) {
        if (!TaczCompat.LOADED || !ClientConfig.isTurnWithGunFix())
            return false;

        return entity.getVehicle() != null
                ? ClientConfig.isOnVehicle()
                : ClientConfig.isOutsideVehicle() && isUpright(entity);
    }

    /**
     * Returns how far the upper body turns while holding a gun, limited by the config.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the upper body turn in degrees, positive toward the same side as vanilla's head yaw
     */
    public static float gunTurnDegrees(LivingEntity entity, float partialTick) {
        float yaw = headYaw(entity, partialTick);

        if (entity.getVehicle() != null) {
            float max = ClientConfig.onVehicleMaxTurnDegrees();
            return Mth.clamp(yaw, -max, max);
        }
        float max = ClientConfig.outsideVehicleMaxTurnDegrees();
        return Mth.clamp(Mth.clamp(yaw, -TurnSettings.TACZ_MAX_DEGREES, TurnSettings.TACZ_MAX_DEGREES), -max, max);
    }

    /**
     * Tells whether the entity is upright: the standing or crouching pose, which also covers walking, running and
     * creative flying, but not swimming, elytra flying or sleeping.
     *
     * @param entity the entity
     * @return true in the standing or crouching pose
     */
    public static boolean isUpright(Entity entity) {
        Pose pose = entity.getPose();
        return pose == Pose.STANDING || pose == Pose.CROUCHING;
    }

    /**
     * Turns a part rotation around the vertical axis. Does not use JOML's angle conversion, which is faulty in the
     * version Minecraft ships.
     *
     * @param x   the x angle in radians
     * @param y   the y angle in radians
     * @param z   the z angle in radians
     * @param yaw the turn in radians
     * @return the turned x, y and z angles in radians
     */
    public static float[] turnRotation(float x, float y, float z, float yaw) {
        float sa = Mth.sin(x);
        float ca = Mth.cos(x);
        float sb = Mth.sin(y);
        float cb = Mth.cos(y);
        float sc = Mth.sin(z);
        float cc = Mth.cos(z);
        float st = Mth.sin(yaw);
        float ct = Mth.cos(yaw);

        // The part's rotation matrix R = Rz(z) * Ry(y) * Rx(x), the entries that are needed.
        float r00 = cc * cb;
        float r01 = cc * sb * sa - sc * ca;
        float r02 = cc * sb * ca + sc * sa;
        float r10 = sc * cb;
        float r11 = sc * sb * sa + cc * ca;
        float r20 = -sb;
        float r21 = cb * sa;
        float r22 = cb * ca;

        // M = Ry(yaw) * R. Ry only mixes rows 0 and 2, row 1 stays.
        float m00 = ct * r00 + st * r20;
        float m01 = ct * r01 + st * r21;
        float m10 = r10;
        float m11 = r11;
        float m20 = -st * r00 + ct * r20;
        float m21 = -st * r01 + ct * r21;
        float m22 = -st * r02 + ct * r22;

        float targetY = y + yaw;
        float sinY = Mth.clamp(-m20, -1.0F, 1.0F);

        if (Math.abs(sinY) > 0.99999F) {
            float ay = sinY > 0.0F ? Mth.HALF_PI : -Mth.HALF_PI;
            float ax = sinY > 0.0F
                    ? z + (float) Math.atan2(m01, m11)
                    : (float) Math.atan2(-m01, m11) - z;

            return new float[]{x + wrap(ax - x), targetY + wrap(ay - targetY), z};
        }

        float ax = (float) Math.atan2(m21, m22);
        float ay = (float) Math.asin(sinY);
        float az = (float) Math.atan2(m10, m00);
        float bx = ax + Mth.PI;
        float by = Mth.PI - ay;
        float bz = az + Mth.PI;

        if (Math.abs(wrap(bx - x)) + Math.abs(wrap(bz - z)) < Math.abs(wrap(ax - x)) + Math.abs(wrap(az - z))) {
            ax = bx;
            ay = by;
            az = bz;
        }

        return new float[]{x + wrap(ax - x), targetY + wrap(ay - targetY), z + wrap(az - z)};
    }

    /**
     * Turns a part pose: moves its pivot around a vertical axis and turns its rotation.
     *
     * @param pose        the pose
     * @param pivotYaw    the turn of the pivot in radians
     * @param rotationYaw the turn of the rotation in radians
     * @param axisX       the x of the vertical axis
     * @param axisZ       the z of the vertical axis
     * @return the turned pose
     */
    public static PartPose turnPose(PartPose pose, float pivotYaw, float rotationYaw, float axisX, float axisZ) {
        float cos = Mth.cos(pivotYaw);
        float sin = Mth.sin(pivotYaw);
        float dx = pose.x - axisX;
        float dz = pose.z - axisZ;
        float x = axisX + dx * cos + dz * sin;
        float z = axisZ - dx * sin + dz * cos;

        if (rotationYaw == 0.0F) {
            return PartPose.offsetAndRotation(x, pose.y, z, pose.xRot, pose.yRot, pose.zRot);
        }

        float[] turned = turnRotation(pose.xRot, pose.yRot, pose.zRot, rotationYaw);
        return PartPose.offsetAndRotation(x, pose.y, z, turned[0], turned[1], turned[2]);
    }

    /**
     * Compares two part poses value by value.
     *
     * @param a the first pose
     * @param b the second pose
     * @return true when all offsets and rotations are equal
     */
    public static boolean samePose(PartPose a, PartPose b) {
        return a.x == b.x
                && a.y == b.y
                && a.z == b.z
                && a.xRot == b.xRot
                && a.yRot == b.yRot
                && a.zRot == b.zRot;
    }

    /**
     * Wraps an angle to the range from -PI to PI.
     *
     * @param radians the angle
     * @return the same angle within -PI to PI
     */
    public static float wrap(float radians) {
        return (float) Math.atan2(Math.sin(radians), Math.cos(radians));
    }
}

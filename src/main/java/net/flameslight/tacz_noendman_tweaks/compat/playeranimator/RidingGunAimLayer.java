package net.flameslight.tacz_noendman_tweaks.compat.playeranimator;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.client.PoseDebug;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

/**
 * Player Animator layer that turns the gun holding pose of a riding player toward where the player looks, on any
 * vehicle: boats, minecarts, horses and other mounts, and vehicles from other mods.
 * <p>
 * TACZ's rotation layer gives the head and arms pitch only and turns the whole player toward the camera instead,
 * but it skips that turn whenever the player has a vehicle, so the gun keeps pointing where the body faces. This
 * layer runs right after TACZ's layers and does that turn for the upper body only: torso, head and arms turn as
 * one block around the player's vertical axis, so the hands stay on the gun and the arms on the shoulders. The
 * legs keep TACZ's riding pose. Looking straight ahead gives a turn of 0, which leaves TACZ's pose exactly as it
 * is. Only loaded when Player Animator is installed.
 */
public final class RidingGunAimLayer implements IAnimation {

    /** Id of this layer in Player Animator's per-player data. */
    public static final ResourceLocation ID = new ResourceLocation(TaczNoEndManTweaks.MOD_ID, "riding_gun_aim");

    /** Layer priority. TACZ's layers use 93 to 96, so this one is applied after them. */
    public static final int PRIORITY = 97;

    /** Largest head-to-body angle, in degrees, the upper body follows. Same limit as TACZ and vanilla riding. */
    public static final float MAX_AIM_DEGREES = 85.0F;

    private final AbstractClientPlayer player;

    /**
     * Creates the layer of one player.
     *
     * @param player the player
     */
    private RidingGunAimLayer(AbstractClientPlayer player) {
        this.player = player;
    }

    /**
     * Registers the layer with Player Animator, so every player gets one. Called once during client setup.
     */
    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ID, PRIORITY, RidingGunAimLayer::new);
    }

    /**
     * Tells whether the layer changes the pose: whenever TACZ skips its turn, so while the player has a vehicle of
     * any kind and holds a gun TACZ animates.
     *
     * @return true when the player has a vehicle and TACZ's rotation layer is active
     */
    @Override
    public boolean isActive() {
        return this.player.getVehicle() != null && PlayerAnimatorCheck.isTaczGunPose(this.player);
    }

    /**
     * Not used: the turn is computed from the player's rotation on every call.
     *
     * @param tickDelta the partial tick
     */
    @Override
    public void setupAnim(float tickDelta) {
    }

    /**
     * Turns the position and rotation of the torso, head and arms around the player's vertical axis. Every other
     * part and the bends pass through unchanged.
     *
     * @param modelName the Player Animator part name
     * @param type      the transform type
     * @param tickDelta the partial tick
     * @param value     the value the lower layers produced
     * @return the turned value
     */
    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value) {
        if (type == TransformType.BEND || !isUpperBody(modelName)) {
            return value;
        }
        float yaw = Mth.clamp(netHeadYaw(this.player, tickDelta), -MAX_AIM_DEGREES, MAX_AIM_DEGREES) * Mth.DEG_TO_RAD;
        if (yaw == 0.0F) {
            return value;
        }
        Vec3f turned = type == TransformType.POSITION ? turnPosition(value, yaw) : turnRotation(value, yaw);
        logTransform(modelName, type, tickDelta, yaw, value, turned);
        return turned;
    }

    /**
     * Logs one part's turn in debug mode, at most once per second per part and type. For rotations it also logs
     * how far, in degrees, the written angles are from the exact turned orientation, which should be close to 0.
     *
     * @param modelName the Player Animator part name
     * @param type      the transform type
     * @param tickDelta the partial tick
     * @param yaw       the turn in radians
     * @param in        the value the lower layers produced
     * @param out       the turned value
     */
    private void logTransform(String modelName, TransformType type, float tickDelta, float yaw, Vec3f in, Vec3f out) {
        if (PoseDebug.shouldLog(this.player, "layer state")) {
            ModLogger.debug("riding gun aim layer: vehicle {}, net head yaw {}, body yaw {}, head yaw {}, pitch {}, turn {}",
                    this.player.getVehicle().getClass().getName(), netHeadYaw(this.player, tickDelta),
                    Mth.rotLerp(tickDelta, this.player.yBodyRotO, this.player.yBodyRot),
                    Mth.rotLerp(tickDelta, this.player.yHeadRotO, this.player.yHeadRot),
                    Mth.lerp(tickDelta, this.player.xRotO, this.player.getXRot()), yaw * Mth.RAD_TO_DEG);
        }
        if (!PoseDebug.shouldLog(this.player, "layer " + modelName + " " + type)) {
            return;
        }
        if (type == TransformType.POSITION) {
            ModLogger.debug("riding gun aim layer: {} position in {}, out {}", modelName,
                    PoseDebug.offsets(in.getX(), in.getY(), in.getZ()), PoseDebug.offsets(out.getX(), out.getY(), out.getZ()));
            return;
        }
        Quaternionf expected = new Quaternionf().rotationY(yaw).mul(new Quaternionf().rotationZYX(in.getZ(), in.getY(), in.getX()));
        Quaternionf written = new Quaternionf().rotationZYX(out.getZ(), out.getY(), out.getX());
        float error = 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(expected.dot(written)))) * Mth.RAD_TO_DEG;
        ModLogger.debug("riding gun aim layer: {} rotation in {}, out {}, conversion error {} degrees", modelName,
                PoseDebug.angles(in.getX(), in.getY(), in.getZ()), PoseDebug.angles(out.getX(), out.getY(), out.getZ()), error);
    }

    /**
     * Returns the head yaw relative to the body the player is drawn with, computed the same way as
     * LivingEntityRenderer.render does before it calls setupAnim. On a living mount (horse, pig, strider, camel)
     * the body is drawn facing the mount, with the head limited to 85 degrees and the body eased toward the head
     * past 50 degrees. On any other vehicle the player's own body yaw is used, which the vehicle keeps (a boat
     * sets it to its own yaw).
     *
     * @param entity      the riding entity
     * @param partialTick the partial tick of this frame
     * @return the angle in degrees, positive toward the same side as vanilla's head yaw
     */
    private static float netHeadYaw(LivingEntity entity, float partialTick) {
        float body = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float head = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle.shouldRiderSit() && vehicle instanceof LivingEntity mount) {
            body = Mth.rotLerp(partialTick, mount.yBodyRotO, mount.yBodyRot);
            float clamped = Mth.clamp(Mth.wrapDegrees(head - body), -85.0F, 85.0F);
            body = head - clamped;
            if (clamped * clamped > 2500.0F) {
                body += clamped * 0.2F;
            }
        }
        float yaw = Mth.wrapDegrees(head - body);
        return LivingEntityRenderer.isEntityUpsideDown(entity) ? -yaw : yaw;
    }

    /**
     * Tells whether a part belongs to the upper body that turns.
     *
     * @param modelName the Player Animator part name
     * @return true for the torso, head and both arms
     */
    private static boolean isUpperBody(String modelName) {
        return "torso".equals(modelName) || "head".equals(modelName)
                || "rightArm".equals(modelName) || "leftArm".equals(modelName);
    }

    /**
     * Moves a part pivot around the vertical axis through the model origin, the axis TACZ turns the whole player
     * around.
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
     * Turns a part rotation around the vertical axis: the rotation becomes turn * current, converted back to the
     * ZYX angles ModelPart uses (rotation = Rz * Ry * Rx), so tilted parts such as TACZ's left arm keep their tilt.
     * <p>
     * The conversion reads the angles straight from the rotation matrix. JOML's getEulerAnglesZYX is not used:
     * in the JOML version Minecraft ships, its angles do not rebuild the same rotation through rotationZYX, which
     * threw the arms tens of degrees off. Away from 90 degrees of yaw the angles have two equivalent solutions,
     * and the one closest to the part's original angles is used, because mods that read the angles one by one
     * (EMF) draw the other one upside down. At exactly 90 degrees of yaw only x - z or x + z is defined, so the
     * original z angle is kept and x takes the rest.
     *
     * @param rotation the rotation in radians, as x, y and z angles
     * @param yaw      the turn in radians
     * @return the turned rotation
     */
    private static Vec3f turnRotation(Vec3f rotation, float yaw) {
        float x = rotation.getX();
        float y = rotation.getY();
        float z = rotation.getZ();
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
            float ax = sinY > 0.0F ? z + (float) Math.atan2(m01, m11) : (float) Math.atan2(-m01, m11) - z;
            return new Vec3f(x + wrap(ax - x), targetY + wrap(ay - targetY), z);
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
        return new Vec3f(x + wrap(ax - x), targetY + wrap(ay - targetY), z + wrap(az - z));
    }

    /**
     * Wraps an angle to the range from -PI to PI.
     *
     * @param radians the angle
     * @return the same angle within -PI to PI
     */
    private static float wrap(float radians) {
        return (float) Math.atan2(Math.sin(radians), Math.cos(radians));
    }
}

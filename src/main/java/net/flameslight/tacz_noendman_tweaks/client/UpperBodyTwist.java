package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCheck;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Twists a player model's torso and arms sideways toward where the player looks, on top of whatever pose
 * vanilla, Player Animator, EMF or other mods already set. The twist is applied right after the model is set up,
 * applied again to the parts EMF animates or copies a pose onto (EMF draws through its own part objects), and
 * every original pose is restored after the player is drawn. Render thread only.
 * <p>
 * Not applied while TACZ animates a held gun: RidingGunAimLayer turns that pose inside Player Animator instead.
 */
public final class UpperBodyTwist {

    /** Largest head-to-body angle, in degrees, that the torso follows. */
    public static final float MAX_TWIST_DEGREES = 60.0F;

    /** Share of that angle the torso takes. The head turns the rest, as in vanilla. */
    public static final float TORSO_SHARE = 0.6F;

    private static final int BODY = 0;
    private static final int HEAD = 3;
    private static final int PART_COUNT = 4;

    private static final boolean PLAYER_ANIMATOR_LOADED = ModList.get().isLoaded(TaczNoEndManTweaks.PLAYER_ANIMATOR_ID);

    /*
        Players currently being drawn with a twist. A stack, in case one player render happens inside another.
     */
    private static final Deque<Pass> ACTIVE = new ArrayDeque<>();

    /**
     * Returns the torso twist for a frame, before the fade strength is applied.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the twist in degrees, positive toward the same side as vanilla's head yaw
     */
    public static float twistDegrees(LivingEntity entity, float partialTick) {
        return Mth.clamp(headBodyDegrees(entity, partialTick), -MAX_TWIST_DEGREES, MAX_TWIST_DEGREES) * TORSO_SHARE;
    }

    /**
     * Returns the head-to-body yaw for a frame.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the angle in degrees, from -180 to 180
     */
    private static float headBodyDegrees(LivingEntity entity, float partialTick) {
        float body = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float head = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        return Mth.wrapDegrees(head - body);
    }

    /**
     * Applies the twist to a player model that was just set up for rendering. Does nothing when the player's
     * twist strength is 0, while TACZ animates a held gun, or for the local player in first person.
     *
     * @param player      the player being drawn
     * @param model       the player's model, already set up for this frame
     * @param partialTick the partial tick of this frame
     */
    public static void begin(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        float strength = TwistFade.strength(player, partialTick);
        if (strength <= 0.0F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (PLAYER_ANIMATOR_LOADED && PlayerAnimatorCheck.isTaczGunPose(player)) {
            // TACZ's gun pose is turned by RidingGunAimLayer inside Player Animator.
            return;
        }
        float torso = twistDegrees(player, partialTick) * strength * Mth.DEG_TO_RAD;
        if (torso == 0.0F) {
            return;
        }
        Pass pass = new Pass(player, torso, torso, 0.0F);
        pass.reapplyOn(model.body, model.rightArm, model.leftArm, model.head,
                model.jacket, model.rightSleeve, model.leftSleeve, model.hat);
        ACTIVE.push(pass);
        ModLogger.debugOnce("upper body twist applied to a player model");
    }

    /**
     * Restores every pose the twist changed after the entity was drawn, if a twist was applied to it.
     *
     * @param entity the entity that was drawn
     */
    public static void end(LivingEntity entity) {
        Pass top = ACTIVE.peek();
        if (top != null && top.entity == entity) {
            ACTIVE.pop();
            top.restore();
        }
    }

    /**
     * Applies the twist to a set of parts another mod animated or posed after the twist was first applied, such as
     * the part objects EMF draws with. Parts that still hold the twist are not twisted twice. Body and arms are
     * required, every other part may be null when the model has no such part.
     *
     * @param body        the body part
     * @param rightArm    the right arm part
     * @param leftArm     the left arm part
     * @param head        the head part
     * @param jacket      the jacket layer, copied from the body
     * @param rightSleeve the right sleeve layer, copied from the right arm
     * @param leftSleeve  the left sleeve layer, copied from the left arm
     * @param hat         the hat layer, copied from the head
     */
    public static void reapplyOn(ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart head,
                                 ModelPart jacket, ModelPart rightSleeve, ModelPart leftSleeve, ModelPart hat) {
        Pass top = ACTIVE.peek();
        if (top != null && body != null && rightArm != null && leftArm != null) {
            top.reapplyOn(body, rightArm, leftArm, head, jacket, rightSleeve, leftSleeve, hat);
        }
    }

    /**
     * One twisted render of one player. Holds every set of parts the twist was applied to: the PlayerModel's own
     * parts and any separate part objects another mod (EMF) draws with.
     */
    private static final class Pass {

        private final LivingEntity entity;
        private final float cos;
        private final float sin;
        private final Quaternionf[] turns = new Quaternionf[PART_COUNT];
        private final float[] angles = new float[PART_COUNT];
        private final List<PartSet> sets = new ArrayList<>();

        /**
         * Creates a pass for one player render.
         *
         * @param entity the player
         * @param torso  the torso turn in radians, also used to move the arm pivots around the body
         * @param arms   the arm turn in radians
         * @param head   the extra head turn in radians
         */
        private Pass(LivingEntity entity, float torso, float arms, float head) {
            this.entity = entity;
            this.cos = Mth.cos(torso);
            this.sin = Mth.sin(torso);
            this.angles[BODY] = torso;
            this.angles[1] = arms;
            this.angles[2] = arms;
            this.angles[HEAD] = head;
            for (int i = 0; i < PART_COUNT; i++) {
                this.turns[i] = new Quaternionf().rotationY(this.angles[i]);
            }
        }

        /**
         * Twists a set of parts. A set seen before only gets parts that changed since twisted again. A new set takes
         * over the untwisted pose of the first set for any part that already holds the first set's twisted pose
         * (a mod copied it over), so that part is not twisted twice.
         *
         * @param body        the body part
         * @param rightArm    the right arm part
         * @param leftArm     the left arm part
         * @param head        the head part, or null
         * @param jacket      the jacket layer, or null
         * @param rightSleeve the right sleeve layer, or null
         * @param leftSleeve  the left sleeve layer, or null
         * @param hat         the hat layer, or null
         */
        private void reapplyOn(ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart head,
                               ModelPart jacket, ModelPart rightSleeve, ModelPart leftSleeve, ModelPart hat) {
            for (PartSet set : this.sets) {
                if (set.parts[BODY] == body) {
                    set.reapplyIfChanged();
                    return;
                }
            }
            PartSet first = this.sets.isEmpty() ? null : this.sets.get(0);
            PartSet set = new PartSet(new ModelPart[]{body, rightArm, leftArm, head},
                    new ModelPart[]{jacket, rightSleeve, leftSleeve, hat});
            for (int i = 0; i < PART_COUNT; i++) {
                if (set.parts[i] == null) {
                    continue;
                }
                PartPose current = set.parts[i].storePose();
                set.restorePose[i] = current;
                set.base[i] = first != null && first.applied[i] != null && samePose(current, first.applied[i])
                        ? first.base[i] : current;
                if (set.layers[i] != null) {
                    set.layerRestorePose[i] = set.layers[i].storePose();
                }
            }
            set.twistAll();
            this.sets.add(set);
        }

        /**
         * Puts every changed part back to the pose it had before the twist touched it, newest set first.
         */
        private void restore() {
            for (int s = this.sets.size() - 1; s >= 0; s--) {
                this.sets.get(s).restore();
            }
        }

        /**
         * Turns one part pose. The pivot moves around the vertical axis through the body pivot by the torso turn,
         * so arms stay on the shoulders, and the part's rotation is composed as turn * current, then converted back
         * to the ZYX angles ModelPart uses, so tilted parts keep their tilt.
         * <p>
         * Those angles have two equivalent solutions. Past 90 degrees of yaw the plain conversion returns the one with
         * X and Z flipped by 180 degrees: the same orientation, but mods that read or blend the angles one by one
         * (EMF, its compat addons) then draw the part upside down. So the solution closest to the part's original
         * angles is used.
         *
         * @param pose   the untwisted pose
         * @param turn   the part's turn
         * @param angle  the part's turn angle in radians
         * @param pivotX the body pivot x
         * @param pivotZ the body pivot z
         * @return the twisted pose
         */
        private PartPose twist(PartPose pose, Quaternionf turn, float angle, float pivotX, float pivotZ) {
            float dx = pose.x - pivotX;
            float dz = pose.z - pivotZ;
            float x = pivotX + dx * this.cos + dz * this.sin;
            float z = pivotZ - dx * this.sin + dz * this.cos;
            if (angle == 0.0F) {
                return PartPose.offsetAndRotation(x, pose.y, z, pose.xRot, pose.yRot, pose.zRot);
            }
            Quaternionf rotation = new Quaternionf(turn).mul(new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot));
            Vector3f euler = rotation.getEulerAnglesZYX(new Vector3f());
            float ax = euler.x;
            float ay = euler.y;
            float az = euler.z;
            float bx = ax + Mth.PI;
            float by = Mth.PI - ay;
            float bz = az + Mth.PI;
            float distanceA = Math.abs(wrap(ax - pose.xRot)) + Math.abs(wrap(az - pose.zRot));
            float distanceB = Math.abs(wrap(bx - pose.xRot)) + Math.abs(wrap(bz - pose.zRot));
            float targetY = pose.yRot + angle;
            if (distanceB < distanceA) {
                ax = bx;
                ay = by;
                az = bz;
            }
            return PartPose.offsetAndRotation(x, pose.y, z,
                    pose.xRot + wrap(ax - pose.xRot), targetY + wrap(ay - targetY), pose.zRot + wrap(az - pose.zRot));
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

        /**
         * Compares two poses value by value.
         *
         * @param a the first pose
         * @param b the second pose
         * @return true when all offsets and rotations are equal
         */
        private static boolean samePose(PartPose a, PartPose b) {
            return a.x == b.x && a.y == b.y && a.z == b.z
                    && a.xRot == b.xRot && a.yRot == b.yRot && a.zRot == b.zRot;
        }

        /**
         * One set of body, arms, head and their outer layers, with the poses needed to twist and restore it.
         */
        private final class PartSet {

            private final ModelPart[] parts;
            private final ModelPart[] layers;
            private final PartPose[] base = new PartPose[PART_COUNT];
            private final PartPose[] applied = new PartPose[PART_COUNT];
            private final PartPose[] restorePose = new PartPose[PART_COUNT];
            private final PartPose[] layerRestorePose = new PartPose[PART_COUNT];

            /**
             * Creates a part set.
             *
             * @param parts  body, right arm, left arm and head, the head may be null
             * @param layers jacket, right sleeve, left sleeve and hat, any of them may be null
             */
            private PartSet(ModelPart[] parts, ModelPart[] layers) {
                this.parts = parts;
                this.layers = layers;
            }

            /**
             * Takes every part that changed since the twist was applied as its new untwisted pose, then twists again.
             */
            private void reapplyIfChanged() {
                boolean changed = false;
                for (int i = 0; i < PART_COUNT; i++) {
                    if (this.parts[i] == null) {
                        continue;
                    }
                    PartPose current = this.parts[i].storePose();
                    if (!samePose(current, this.applied[i])) {
                        this.base[i] = current;
                        changed = true;
                    }
                }
                if (changed) {
                    twistAll();
                }
            }

            /**
             * Twists every part from its untwisted pose, and copies the result to the outer skin layers, like
             * vanilla's setupAnim does.
             */
            private void twistAll() {
                float pivotX = this.base[BODY].x;
                float pivotZ = this.base[BODY].z;
                for (int i = 0; i < PART_COUNT; i++) {
                    if (this.parts[i] == null) {
                        continue;
                    }
                    if (angles[i] == 0.0F && (i == HEAD || angles[BODY] == 0.0F)) {
                        // Nothing turns this part: leave it exactly as it is.
                        this.applied[i] = this.base[i];
                        continue;
                    }
                    this.applied[i] = twist(this.base[i], turns[i], angles[i], pivotX, pivotZ);
                    this.parts[i].loadPose(this.applied[i]);
                    if (this.layers[i] != null) {
                        this.layers[i].copyFrom(this.parts[i]);
                    }
                }
            }

            /**
             * Puts the poses back that the parts had before this set was twisted.
             */
            private void restore() {
                for (int i = 0; i < PART_COUNT; i++) {
                    if (this.parts[i] == null) {
                        continue;
                    }
                    this.parts[i].loadPose(this.restorePose[i]);
                    if (this.layers[i] != null && this.layerRestorePose[i] != null) {
                        this.layers[i].loadPose(this.layerRestorePose[i]);
                    }
                }
            }
        }
    }
}

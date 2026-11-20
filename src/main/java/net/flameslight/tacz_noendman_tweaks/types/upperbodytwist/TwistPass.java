package net.flameslight.tacz_noendman_tweaks.types.upperbodytwist;

import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * One twisted render of one player, used by UpperBodyTwist.
 */
public final class TwistPass {

    private final LivingEntity entity;
    private final float[] angles = new float[PartSet.PART_COUNT];
    private final List<PartSet> sets = new ArrayList<>();
    private final float handover;
    private final float gunTurn;
    private final float headTarget;
    private final boolean afterEmf;

    /**
     * Creates a pass for one player render.
     *
     * @param entity     the player
     * @param torso      the torso turn in radians
     * @param arms       the arm turn in radians
     * @param handover   how much of the gun pose's turn is still kept, 1 for all, 0 for none
     * @param gunTurn    the gun pose's upper body turn in radians
     * @param headTarget the yaw in radians the head faces
     * @param afterEmf   true to twist only after EMF's animation
     */
    public TwistPass(LivingEntity entity, float torso, float arms, float handover, float gunTurn, float headTarget,
                     boolean afterEmf) {
        this.entity = entity;
        this.angles[PartSet.BODY] = torso;
        this.angles[1] = arms;
        this.angles[2] = arms;
        this.handover = handover;
        this.gunTurn = gunTurn;
        this.headTarget = headTarget;
        this.afterEmf = afterEmf;
    }

    /**
     * Returns the player of this render.
     *
     * @return the player
     */
    public LivingEntity getEntity() {
        return this.entity;
    }

    /**
     * Returns the turn of one part.
     *
     * @param index the part index
     * @return the turn in radians
     */
    float getAngle(int index) {
        return this.angles[index];
    }

    /**
     * Returns how much of the gun pose's turn is still kept.
     *
     * @return 1 for all of the gun turn, 0 for none of it
     */
    float getHandover() {
        return this.handover;
    }

    /**
     * Returns the head turn, which is above 0 only while some of the gun pose's turn is still kept.
     *
     * @param head the head's untwisted pose
     * @return the turn in radians
     */
    float headAngle(PartPose head) {
        return this.handover <= 0.0F
                ? 0.0F
                : this.handover * TurnMathUtils.headTurn(head.yRot, this.headTarget, this.gunTurn);
    }

    /**
     * Twists a set of parts, without twisting any part twice.
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
    public void reapplyOn(ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart head,
                          ModelPart jacket, ModelPart rightSleeve, ModelPart leftSleeve, ModelPart hat) {
        for (PartSet set : this.sets) {
            if (set.isFor(body)) {
                set.reapplyIfChanged();

                return;
            }
        }

        PartSet first = this.sets.isEmpty() ? null : this.sets.get(0);
        PartSet set = new PartSet(this, new ModelPart[]{body, rightArm, leftArm, head},
                new ModelPart[]{jacket, rightSleeve, leftSleeve, hat});
        set.capture(first);

        if (first == null && this.afterEmf) {
            // Twisted after EMF's animation.
            set.markPending();
        } else {
            set.twistAll();
        }

        this.sets.add(set);
    }

    /**
     * Twists again the parts another mod posed since the last twist, in every set.
     */
    public void reapply() {
        for (PartSet set : this.sets) {
            set.reapplyIfChanged();
        }
    }

    /**
     * Restores every changed part.
     */
    public void restore() {
        for (int s = this.sets.size() - 1; s >= 0; s--) {
            this.sets.get(s).restore();
        }
    }

    /**
     * Turns one part pose: its pivot by the torso turn and its rotation by the part's own angle.
     *
     * @param pose   the untwisted pose
     * @param angle  the part's turn angle in radians
     * @param pivotX the x of the turn axis
     * @param pivotZ the z of the turn axis
     * @return the twisted pose
     */
    PartPose twist(PartPose pose, float angle, float pivotX, float pivotZ) {
        return TurnMathUtils.turnPose(pose, this.angles[PartSet.BODY], angle, pivotX, pivotZ);
    }
}

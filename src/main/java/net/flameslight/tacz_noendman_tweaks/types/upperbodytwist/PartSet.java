package net.flameslight.tacz_noendman_tweaks.types.upperbodytwist;

import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/**
 * One set of body, arms, head and their outer layers twisted by a TwistPass.
 */
final class PartSet {

    /** Index of the body part. */
    static final int BODY = 0;

    /** Index of the head part. */
    static final int HEAD = 3;

    /** Number of parts in a set. */
    static final int PART_COUNT = 4;

    private final TwistPass pass;
    private final ModelPart[] parts;
    private final ModelPart[] layers;
    private final PartPose[] base = new PartPose[PART_COUNT];
    private final PartPose[] applied = new PartPose[PART_COUNT];
    private final PartPose[] restorePose = new PartPose[PART_COUNT];
    private final PartPose[] layerRestorePose = new PartPose[PART_COUNT];

    /*
        True after untwist, so the next reapplyIfChanged twists again even where nothing changed.
     */
    private boolean untwisted;

    /**
     * Creates a part set.
     *
     * @param pass   the pass the set belongs to
     * @param parts  body, right arm, left arm and head, the head may be null
     * @param layers jacket, right sleeve, left sleeve and hat, any of them may be null
     */
    PartSet(TwistPass pass, ModelPart[] parts, ModelPart[] layers) {
        this.pass = pass;
        this.parts = parts;
        this.layers = layers;
    }

    /**
     * Tells whether this set holds the given body part.
     *
     * @param body the body part
     * @return true when it is this set's body
     */
    boolean isFor(ModelPart body) {
        return this.parts[BODY] == body;
    }

    /**
     * Records the current poses to twist and restore, without twisting a part the first set already twisted.
     *
     * @param first the pass's first set, or null when this is the first
     */
    void capture(PartSet first) {
        for (int i = 0; i < PART_COUNT; i++) {
            if (this.parts[i] == null) {
                continue;
            }

            PartPose current = this.parts[i].storePose();
            this.restorePose[i] = current;
            this.base[i] = first != null && first.applied[i] != null && TurnMathUtils.samePose(current, first.applied[i])
                    ? first.base[i]
                    : current;
            if (this.layers[i] != null) {
                this.layerRestorePose[i] = this.layers[i].storePose();
            }

            this.applied[i] = this.base[i];
        }
    }

    /**
     * Puts the untwisted poses back, so another mod (EMF) reads the vanilla pose. The next reapplyIfChanged twists
     * again.
     */
    void untwist() {
        for (int i = 0; i < PART_COUNT; i++) {
            if (this.parts[i] == null) {
                continue;
            }

            this.parts[i].loadPose(this.base[i]);
            this.applied[i] = this.base[i];

            if (this.layers[i] != null)
                this.layers[i].copyFrom(this.parts[i]);
        }

        this.untwisted = true;
    }

    /**
     * Twists again the parts that changed since the last twist.
     */
    void reapplyIfChanged() {
        boolean changed = this.untwisted;

        this.untwisted = false;

        for (int i = 0; i < PART_COUNT; i++) {
            if (this.parts[i] == null) {
                continue;
            }

            PartPose current = this.parts[i].storePose();
            if (!TurnMathUtils.samePose(current, this.applied[i])) {
                this.base[i] = current;
                changed = true;
            }
        }

        if (changed) {
            twistAll();
        }
    }

    /**
     * Twists every part from its untwisted pose and copies the result to the outer layers.
     */
    void twistAll() {
        float share = 1.0F - this.pass.getHandover();
        float pivotX = this.base[BODY].x * share;
        float pivotZ = this.base[BODY].z * share;

        for (int i = 0; i < PART_COUNT; i++) {
            if (this.parts[i] == null) {
                continue;
            }

            float angle = i == HEAD ? this.pass.headAngle(this.base[i]) : this.pass.getAngle(i);
            if (angle == 0.0F && this.pass.getAngle(BODY) == 0.0F) {
                // Nothing turns this part: leave it exactly as it is.
                this.applied[i] = this.base[i];
                continue;
            }

            this.applied[i] = this.pass.twist(this.base[i], angle, pivotX, pivotZ);
            this.parts[i].loadPose(this.applied[i]);
            if (this.layers[i] != null) {
                this.layers[i].copyFrom(this.parts[i]);
            }
        }
    }

    /**
     * Restores the poses the parts had before the twist.
     */
    void restore() {
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

package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Head yaw reduction (head turns too much without it for angles bigger than 85 degrees) of the whole body turn on
 * vehicles, moved after EMF's animation for player models an EMF resource pack animates.
 * Such packs detect vanilla poses (like the bow aim) by comparing the vanilla arm yaw with EMF's head_yaw, which EMF
 * computes from the entity without the reduction. So the reduction is left out of setupAnim, and the head, hat, arms
 * and sleeves yaw are reduced after EMF posed them. Render thread only.
 */
public final class EmfVehicleTurn {

    /*
        Whole body turn, in degrees, over which the head fades from the pack's own head (reduced) to facing where the
        player looks, so it does not snap when the turn starts.
     */
    private static final float HEAD_FADE_DEGREES = 10.0F;

    /*
        The player whose reduction is deferred in the current render (null when none), the reduction in radians, and the
        head yaw relative to the turned body (where the player looks) in radians.
     */
    private static Entity entity = null;
    private static float excessRad = 0.0F;
    private static float headTargetRad = 0.0F;

    /*
        The parts reduced in the current render (head, hat, right arm, left arm, right sleeve, left sleeve) and the yaw
        written to each, so a later EMF pose copy is reduced again but an already reduced part is not.
     */
    private static ModelPart[] reducedParts = null;
    private static final float[] WRITTEN = new float[6];

    /**
     * Starts a render: forgets the previous render's deferred reduction.
     */
    public static void begin() {
        entity = null;
        reducedParts = null;
    }

    /**
     * Tells whether the head yaw reduction of a player is moved after EMF's animation: when an EMF resource pack
     * animates the player model and this mod's own twist is not applied to it (the pack poses it).
     *
     * @param drawn       the entity being drawn
     * @param partialTick the partial tick of this frame
     * @return true to leave the reduction out of setupAnim
     */
    public static boolean shouldDefer(LivingEntity drawn, float partialTick) {
        return drawn instanceof AbstractClientPlayer player
                && TwistFade.strength(player, partialTick) <= 0.0F
                && TwistFade.handover(player, partialTick) <= 0.0F
                && EmfCompat.isModelAnimated(drawn);
    }

    /**
     * Defers the head yaw reduction of the current render to after EMF's animation.
     *
     * @param drawn      the entity being drawn
     * @param excessDeg  the reduction, in degrees, already flipped for upside down entities
     * @param netHeadYaw the head yaw passed to setupAnim, not reduced, in degrees
     */
    public static void defer(Entity drawn, float excessDeg, float netHeadYaw) {
        entity = drawn;
        excessRad = excessDeg * Mth.DEG_TO_RAD;
        headTargetRad = Mth.wrapDegrees(netHeadYaw - excessDeg) * Mth.DEG_TO_RAD;
        reducedParts = null;
    }

    /**
     * Tells whether the reduction applies to a model EMF animated: its main model, while a player with a deferred
     * reduction is being drawn (EMF animates a model during its entity's render, so it is that player's model).
     *
     * @param mainModel whether the animated model is the entity's main model
     * @return true when the reduction applies to this animation
     */
    public static boolean isDeferredFor(boolean mainModel) {
        return mainModel && entity != null;
    }

    /**
     * Applies the reduction after EMF posed the model. The arms and sleeves yaw is reduced. The head and hat fade, over
     * the first HEAD_FADE_DEGREES of the whole body turn, from the pack's head reduced to facing where the player looks
     * relative to the turned body, like vanilla (packs may cap their own head turn, so only reducing it would leave the
     * head behind). A part already done in this render keeps its yaw, so calling again after an EMF pose copy only
     * redoes the parts the copy overwrote.
     *
     * @param head        the head, may be null
     * @param hat         the hat, may be null
     * @param rightArm    the right arm, may be null
     * @param leftArm     the left arm, may be null
     * @param rightSleeve the right sleeve, may be null
     * @param leftSleeve  the left sleeve, may be null
     */
    public static void afterEmf(ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm,
                                ModelPart rightSleeve, ModelPart leftSleeve) {
        if (entity == null || head == null || rightArm == null || leftArm == null)
            return;

        ModelPart[] parts = {head, hat, rightArm, leftArm, rightSleeve, leftSleeve};
        boolean first = reducedParts == null;
        float headFade = Math.min(1.0F, Math.abs(excessRad) / (HEAD_FADE_DEGREES * Mth.DEG_TO_RAD));

        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];

            if (part == null || (!first && reducedParts[i] == part && part.yRot == WRITTEN[i]))
                continue;

            // The head and hat are the first two parts.
            part.yRot = i < 2
                    ? Mth.lerp(headFade, part.yRot - excessRad, headTargetRad)
                    : part.yRot - excessRad;
            WRITTEN[i] = part.yRot;
        }

        reducedParts = parts;
        ModLogger.debugOnce("fix active: vehicle head yaw reduction applied after the EMF animation");
    }

    /**
     * Ends a render.
     *
     * @param drawn the entity that was drawn
     */
    public static void end(Entity drawn) {
        if (drawn == entity) {
            entity = null;
            reducedParts = null;
        }
    }
}

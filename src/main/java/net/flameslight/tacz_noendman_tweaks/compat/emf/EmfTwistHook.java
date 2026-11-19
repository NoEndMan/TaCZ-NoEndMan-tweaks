package net.flameslight.tacz_noendman_tweaks.compat.emf;

import net.flameslight.tacz_noendman_tweaks.client.UpperBodyTwist;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.state.EMFBipedPose;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.models.parts.EMFModelPartVanilla;

import java.util.Map;

/**
 * Entity Model Features hook, done the way EMF Compat Core keeps Not Enough Animations poses: EMF draws through
 * its own part objects and animates them while the model renders, after the twist was first applied. So when an
 * EMF animation ends, the twist is applied to the animated root's own body and arm parts, and when EMF copies its
 * stored biped pose onto a humanoid model, to that model's parts. Only loaded when EMF is installed.
 */
public final class EmfTwistHook extends EMFAnimationApi.EMFAnimationHook {

    /**
     * Applies the twist to the body and arm parts of the root EMF just animated.
     *
     * @param context the animation context
     * @param paused  whether the animation was paused
     */
    @Override
    public void onAnimationEnd(EMFAnimationApi.EMFAnimationHook.AnimationContext context, boolean paused) {
        EMFModelPartRoot root = context.animatingModelRoot();
        if (root == null) {
            return;
        }
        Map<String, EMFModelPartVanilla> parts = root.getAllVanillaPartsByNameEMF();
        UpperBodyTwist.reapplyOn(parts.get("body"), parts.get("right_arm"), parts.get("left_arm"), parts.get("head"),
                parts.get("jacket"), parts.get("right_sleeve"), parts.get("left_sleeve"), parts.get("hat"));
        ModLogger.debugOnce("upper body twist applied after an EMF animation");
    }

    /**
     * Applies the twist after EMF copied its stored biped pose onto a humanoid model, which can happen after
     * the animation itself and would otherwise undo the twist.
     *
     * @param pose   the copied pose
     * @param model  the model the pose was copied to
     * @param paused whether the animation was paused
     */
    @Override
    public void onBipedPoseCopyEnd(EMFBipedPose pose, HumanoidModel<?> model, boolean paused) {
        if (model instanceof PlayerModel<?> player) {
            UpperBodyTwist.reapplyOn(player.body, player.rightArm, player.leftArm, player.head,
                    player.jacket, player.rightSleeve, player.leftSleeve, player.hat);
        } else {
            UpperBodyTwist.reapplyOn(model.body, model.rightArm, model.leftArm, model.head, null, null, null, model.hat);
        }
        ModLogger.debugOnce("upper body twist applied after an EMF biped pose copy");
    }

    /**
     * Registers the hook with EMF.
     */
    public static void register() {
        try {
            EMFAnimationApi.registerAnimationHook(new EmfTwistHook());
        } catch (Exception e) {
            ModLogger.error("could not register the EMF animation hook", e);
        }
    }
}

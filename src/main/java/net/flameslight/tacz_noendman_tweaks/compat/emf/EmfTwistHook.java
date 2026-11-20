package net.flameslight.tacz_noendman_tweaks.compat.emf;

import net.flameslight.tacz_noendman_tweaks.logger.debug.PoseDebug;
import net.flameslight.tacz_noendman_tweaks.client.NoGunTwist;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.state.EMFBipedPose;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.models.parts.EMFModelPartVanilla;

import java.util.Map;

/**
 * EMF animation hook that applies the twist and the gun turn again after EMF re-poses the player model. Only loaded
 * when EMF is installed.
 */
public final class EmfTwistHook extends EMFAnimationApi.EMFAnimationHook {

    /**
     * Applies the twist and the gun turn after an EMF animation.
     *
     * @param context the animation context
     * @param paused  whether the animation was paused
     */
    @Override
    public void onAnimationEnd(EMFAnimationApi.EMFAnimationHook.AnimationContext context, boolean paused) {
        EMFModelPartRoot root = context.animatingModelRoot();

        /* keep just in case it would be found out that this might return null
        if (root == null)
            return;*/

        Map<String, EMFModelPartVanilla> parts = root.getAllVanillaPartsByNameEMF();
        PoseDebug.logBowParts("after EMF animation", parts.get("body"), parts.get("head"), parts.get("right_arm"),
                parts.get("left_arm"));
        NoGunTwist.reapplyOn(parts.get("body"), parts.get("right_arm"), parts.get("left_arm"), parts.get("head"),
                parts.get("jacket"), parts.get("right_sleeve"), parts.get("left_sleeve"), parts.get("hat"));
        EmfGunTwist.afterAnimation(parts.get("body"));
        PoseDebug.logBowParts("after EMF animation and twist", parts.get("body"), parts.get("head"),
                parts.get("right_arm"), parts.get("left_arm"));
        ModLogger.debugOnce("upper body twist applied after an EMF animation");
    }

    /**
     * Applies the twist and the gun turn after EMF copies a stored pose onto the model.
     *
     * @param pose   the copied pose
     * @param model  the model the pose was copied to
     * @param paused whether the animation was paused
     */
    @Override
    public void onBipedPoseCopyEnd(EMFBipedPose pose, HumanoidModel<?> model, boolean paused) {
        if (model instanceof PlayerModel<?> player) {
            NoGunTwist.reapplyOn(player.body, player.rightArm, player.leftArm, player.head,
                    player.jacket, player.rightSleeve, player.leftSleeve, player.hat);
            EmfGunTwist.afterPoseCopy(player.body);
        } else
            NoGunTwist.reapplyOn(model.body, model.rightArm, model.leftArm, model.head, null, null, null, model.hat);

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

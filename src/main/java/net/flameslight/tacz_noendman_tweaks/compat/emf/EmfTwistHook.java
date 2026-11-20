package net.flameslight.tacz_noendman_tweaks.compat.emf;

import net.flameslight.tacz_noendman_tweaks.client.GunTwist;
import net.flameslight.tacz_noendman_tweaks.client.NoGunTwist;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.state.EMFBipedPose;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.models.parts.EMFModelPartVanilla;

import java.util.Map;

/**
 * EMF animation hook: puts the untwisted pose back before EMF animates the player model, and applies the twist and the
 * gun turn again after EMF re-poses it. Only loaded when EMF is installed.
 */
public final class EmfTwistHook extends EMFAnimationApi.EMFAnimationHook {

    /**
     * Puts the untwisted pose back before an EMF animation, so the animation reads the vanilla pose.
     *
     * @param context the animation context
     * @param paused  whether the animation is paused
     * @return true, so the animation runs
     */
    @Override
    public boolean onAnimationStart(EMFAnimationApi.EMFAnimationHook.AnimationContext context, boolean paused) {
        ModelPart body = context.animatingModelRoot().getAllVanillaPartsByNameEMF().get("body");
        NoGunTwist.untwistTop(body);
        GunTwist.beforeAnimation(body);
        return true;
    }

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
        NoGunTwist.reapplyOn(parts.get("body"), parts.get("right_arm"), parts.get("left_arm"), parts.get("head"),
                parts.get("jacket"), parts.get("right_sleeve"), parts.get("left_sleeve"), parts.get("hat"));
        GunTwist.afterAnimation(parts.get("body"));
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
            GunTwist.afterPoseCopy(player.body);
        } else
            NoGunTwist.reapplyOn(model.body, model.rightArm, model.leftArm, model.head, null, null, null, model.hat);

        ModLogger.debugOnce("upper body twist applied after an EMF biped pose copy");
    }

    /**
     * Asks EMF whether it animates a model.
     *
     * @param model the model
     * @return true when EMF animates it
     */
    static boolean isAnimatedByEmf(EntityModel<?> model) {
        return EMFAnimationApi.isModelAnimatedByEMF(model);
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

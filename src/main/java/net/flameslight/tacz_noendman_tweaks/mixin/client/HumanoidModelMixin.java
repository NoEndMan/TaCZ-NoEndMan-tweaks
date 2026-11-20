package net.flameslight.tacz_noendman_tweaks.mixin.client;

import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trident aiming pose fixes: turns the aimed trident toward where the player looks, and with EMF removes vanilla's
 * riding arm offset from the pose, so resource packs that detect it by its arm angle (like Player Extension) also
 * detect it while riding.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    /*
        Vanilla's riding offset on the arms' xRot.
     */
    @Unique
    private static final float RIDING_ARM_OFFSET = -Mth.PI / 5.0F;

    /**
     * Removes the riding offset from the right arm before vanilla applies the trident pose.
     *
     * @param entity the entity being posed
     * @param ci     the callback info
     */
    @Inject(method = "poseRightArm", at = @At("HEAD"))
    private void tacz_noendman$fixRightTridentOffset(LivingEntity entity, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        tacz_noendman$removeRidingOffset(model, model.rightArmPose, model.rightArm);
    }

    /**
     * Removes the riding offset from the left arm before vanilla applies the trident pose.
     *
     * @param entity the entity being posed
     * @param ci     the callback info
     */
    @Inject(method = "poseLeftArm", at = @At("HEAD"))
    private void tacz_noendman$fixLeftTridentOffset(LivingEntity entity, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        tacz_noendman$removeRidingOffset(model, model.leftArmPose, model.leftArm);
    }

    /**
     * Turns the right arm's trident toward where the player looks.
     *
     * @param entity the entity being posed
     * @param ci     the callback info
     */
    @Inject(method = "poseRightArm", at = @At("RETURN"))
    private void tacz_noendman$turnRightTrident(LivingEntity entity, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        tacz_noendman$turnTrident(model, model.rightArmPose, model.rightArm);
    }

    /**
     * Turns the left arm's trident toward where the player looks.
     *
     * @param entity the entity being posed
     * @param ci     the callback info
     */
    @Inject(method = "poseLeftArm", at = @At("RETURN"))
    private void tacz_noendman$turnLeftTrident(LivingEntity entity, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        tacz_noendman$turnTrident(model, model.leftArmPose, model.leftArm);
    }

    /**
     * Removes the riding offset from an arm in the trident pose while riding, with EMF installed.
     *
     * @param model the model
     * @param pose  the arm's pose
     * @param arm   the arm
     */
    @Unique
    private static void tacz_noendman$removeRidingOffset(HumanoidModel<?> model, HumanoidModel.ArmPose pose, ModelPart arm) {
        if (EmfCompat.LOADED
                && model.riding
                && pose == HumanoidModel.ArmPose.THROW_SPEAR
                && ClientConfig.isTridentAimFix())
            arm.xRot -= RIDING_ARM_OFFSET;
    }

    /**
     * Spins an arm in the trident pose around the vertical axis by the head yaw. The arm points up, so this turns the
     * trident toward where the player looks and keeps the arm's raise angle.
     *
     * @param model the model
     * @param pose  the arm's pose
     * @param arm   the arm
     */
    @Unique
    private static void tacz_noendman$turnTrident(HumanoidModel<?> model, HumanoidModel.ArmPose pose, ModelPart arm) {
        if (pose == HumanoidModel.ArmPose.THROW_SPEAR && ClientConfig.isTridentAimFix())
            arm.yRot += model.head.yRot;
    }
}

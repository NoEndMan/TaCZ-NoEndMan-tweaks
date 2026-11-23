package net.flameslight.tacz_noendman_tweaks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.flameslight.tacz_noendman_tweaks.client.EmfVehicleTurn;
import net.flameslight.tacz_noendman_tweaks.client.GunTwist;
import net.flameslight.tacz_noendman_tweaks.client.NoGunTwist;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the upper body twist after the player model is set up, restores the pose when the render ends, and turns
 * the whole player on vehicles past the head turn limit (with the head yaw reduction moved after EMF's animation when
 * an EMF resource pack poses the player, see EmfVehicleTurn).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    private static final String SETUP_ROTATIONS = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V";

    private static final String SETUP_ANIM = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V";

    /*
        The whole body turn of the player being drawn, in degrees, set at the start of each render.
     */
    @Unique
    private float tacz_noendman$bodyExcess;

    /*
        The same turn as it is taken off the head yaw, flipped for upside down entities like vanilla flips the yaw.
     */
    @Unique
    private float tacz_noendman$headExcess;

    /*
        The entity being drawn and the partial tick, for the EMF head yaw reduction check, set at the start of each
        render and cleared at its end.
     */
    @Unique
    private LivingEntity tacz_noendman$entity;
    @Unique
    private float tacz_noendman$partialTick;

    /**
     * Reads the whole body turn of the player being drawn on a vehicle, and starts the render's EMF head yaw state.
     *
     * @param entity      the entity being drawn
     * @param entityYaw   the entity yaw
     * @param partialTick the partial tick
     * @param poseStack   the pose stack
     * @param buffer      the buffer source
     * @param packedLight the light value
     * @param ci          the callback info
     */
    @Inject(method = RENDER, at = @At("HEAD"))
    private void tacz_noendman$readBodyExcess(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                                             MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        this.tacz_noendman$bodyExcess = entity instanceof AbstractClientPlayer
                ? TurnMathUtils.vehicleBodyExcess(entity, partialTick)
                : 0.0F;
        this.tacz_noendman$headExcess = LivingEntityRenderer.isEntityUpsideDown(entity)
                ? -this.tacz_noendman$bodyExcess
                : this.tacz_noendman$bodyExcess;
        this.tacz_noendman$entity = entity;
        this.tacz_noendman$partialTick = partialTick;
        EmfVehicleTurn.begin();
    }

    /**
     * Turns the whole model by the angle past the head turn limit on a vehicle.
     *
     * @param rotationYaw the body yaw vanilla rotates the model by
     * @return the turned body yaw
     */
    @ModifyArg(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ROTATIONS), index = 3)
    private float tacz_noendman$turnWholeBody(float rotationYaw) {
        return rotationYaw + this.tacz_noendman$bodyExcess;
    }

    /**
     * Takes the whole body turn off the head yaw, so the head keeps facing where the player looks. For a player model
     * an EMF resource pack poses (this mod's twist paused), the reduction is applied after EMF's animation instead.
     *
     * @param netHeadYaw the head yaw relative to the body, already flipped for upside down entities
     * @return the head yaw relative to the turned body
     */
    @ModifyArg(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ANIM), index = 4)
    private float tacz_noendman$turnHeadBack(float netHeadYaw) {
        if (this.tacz_noendman$headExcess == 0.0F)
            return netHeadYaw;

        if (EmfVehicleTurn.shouldDefer(this.tacz_noendman$entity, this.tacz_noendman$partialTick)) {
            EmfVehicleTurn.defer(this.tacz_noendman$entity, this.tacz_noendman$headExcess, netHeadYaw);
            return netHeadYaw;
        }

        return netHeadYaw - this.tacz_noendman$headExcess;
    }

    /**
     * Applies the twist to a player model after setupAnim.
     *
     * @param entity      the entity being drawn
     * @param entityYaw   the entity yaw
     * @param partialTick the partial tick
     * @param poseStack   the pose stack
     * @param buffer      the buffer source
     * @param packedLight the light value
     * @param ci          the callback info
     */
    @Inject(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ANIM, shift = At.Shift.AFTER))
    private void tacz_noendman$applyTwist(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                                          MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player
                && ((LivingEntityRenderer<?, ?>) (Object) this).getModel() instanceof PlayerModel<?> model) {
            NoGunTwist.begin(player, model, partialTick);
            GunTwist.begin(player, model, partialTick);
        }
    }

    /**
     * Restores the pose and ends the render's EMF head yaw state when the render ends.
     *
     * @param entity      the entity that was drawn
     * @param entityYaw   the entity yaw
     * @param partialTick the partial tick
     * @param poseStack   the pose stack
     * @param buffer      the buffer source
     * @param packedLight the light value
     * @param ci          the callback info
     */
    @Inject(method = RENDER, at = @At("RETURN"))
    private void tacz_noendman$restoreTwist(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                                            MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        GunTwist.end(entity);
        NoGunTwist.end(entity);
        EmfVehicleTurn.end(entity);
        this.tacz_noendman$entity = null;
    }
}

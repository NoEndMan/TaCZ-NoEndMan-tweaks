package net.flameslight.tacz_noendman_tweaks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfGunTwist;
import net.flameslight.tacz_noendman_tweaks.logger.debug.PoseDebug;
import net.flameslight.tacz_noendman_tweaks.logger.debug.TransitionDebug;
import net.flameslight.tacz_noendman_tweaks.client.NoGunTwist;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the upper body twist after the player model is set up, and restores the pose when the render ends.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

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
    @Inject(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            shift = At.Shift.AFTER))
    private void tacz_noendman$applyTwist(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                                          MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player
                && ((LivingEntityRenderer<?, ?>) (Object) this).getModel() instanceof PlayerModel<?> model) {
            PoseDebug.logModel("after setupAnim", player, model);
            PoseDebug.logBow("after setupAnim", player, model);
            TransitionDebug.afterSetupAnim(player, model);
            NoGunTwist.begin(player, model, partialTick);
            EmfGunTwist.begin(player, model, partialTick);
            PoseDebug.logBow("after twist", player, model);
        }
    }

    /**
     * Restores the pose when the render ends.
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
        if (entity instanceof AbstractClientPlayer player
                && ((LivingEntityRenderer<?, ?>) (Object) this).getModel() instanceof PlayerModel<?> model) {
            PoseDebug.logModel("after render", player, model);
            PoseDebug.logBow("after render", player, model);
            TransitionDebug.afterRender(player, model, partialTick);
        }
        EmfGunTwist.end(entity);
        NoGunTwist.end(entity);
    }
}

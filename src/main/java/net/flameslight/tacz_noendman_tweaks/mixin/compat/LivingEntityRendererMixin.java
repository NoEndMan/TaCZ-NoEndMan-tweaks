package net.flameslight.tacz_noendman_tweaks.mixin.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.flameslight.tacz_noendman_tweaks.client.PoseDebug;
import net.flameslight.tacz_noendman_tweaks.client.UpperBodyTwist;
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
 * Applies the upper body twist right after the model is set up (so after vanilla and Player Animator), and
 * restores the pose when the render ends (so after the armor and held item layers, which follow the twist).
 * Only applied when Immersive Aircraft is installed.
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
            UpperBodyTwist.begin(player, model, partialTick);
        }
    }

    /**
     * Restores the pose when the render ends. In debug mode it first logs the pose the player was drawn with.
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
        }
        UpperBodyTwist.end(entity);
    }
}

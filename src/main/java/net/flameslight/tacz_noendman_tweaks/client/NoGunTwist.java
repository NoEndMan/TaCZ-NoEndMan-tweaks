package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.config.TurnSettings;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.types.upperbodytwist.TwistPass;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The no-gun twist. Render thread only.
 */
public final class NoGunTwist {

    /*
        Players currently being drawn with a twist.
     */
    private static final Deque<TwistPass> ACTIVE = new ArrayDeque<>();

    /**
     * Returns the torso twist for a frame, before the fade strength is applied.
     *
     * @param entity      the entity
     * @param partialTick the partial tick of this frame
     * @return the twist in degrees, positive toward the same side as vanilla's head yaw
     */
    public static float twistDegrees(LivingEntity entity, float partialTick) {
        float max = ClientConfig.maxTurnDegreesFor(entity.getVehicle() != null);

        return Mth.clamp(TurnMathUtils.headYaw(entity, partialTick), -max, max) * TurnSettings.TORSO_SHARE;
    }

    /**
     * Applies the twist to a player model that was just set up for rendering.
     *
     * @param player      the player being drawn
     * @param model       the player's model, already set up for this frame
     * @param partialTick the partial tick of this frame
     */
    public static void begin(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        float strength = TwistFade.strength(player, partialTick);
        float handover = TwistFade.handover(player, partialTick);

        if (strength <= 0.0F && handover <= 0.0F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (PlayerAnimatorCompat.isTaczGunPose(player)) {
            // TaCZ's gun pose is turned by GunTwist.
            return;
        }
        // Right after a gun pose ends, start from the gun pose's turn and hand over to the twist.
        float twist = twistDegrees(player, partialTick) * strength;
        float arms = 0.0F;
        float gunTurn = 0.0F;

        if (handover > 0.0F) {
            gunTurn = TurnMathUtils.gunTurnDegrees(player, partialTick);
            twist = Mth.lerp(handover, twist, gunTurn);
            arms = gunTurn * handover;
        }

        float torso = twist * Mth.DEG_TO_RAD;

        if (torso == 0.0F && arms == 0.0F) {
            return;
        }

        // With EMF, the twist steps aside during EMF's animation and is applied again on its result.
        TwistPass pass = new TwistPass(player, torso, arms * Mth.DEG_TO_RAD, handover, gunTurn * Mth.DEG_TO_RAD,
                TurnMathUtils.headYaw(player, partialTick) * Mth.DEG_TO_RAD);
        pass.reapplyOn(model.body, model.rightArm, model.leftArm, model.head,
                model.jacket, model.rightSleeve, model.leftSleeve, model.hat);
        ACTIVE.push(pass);
        ModLogger.debugOnce("upper body twist applied to a player model");
    }

    /**
     * Restores the poses the twist changed after the entity was drawn.
     *
     * @param entity the entity that was drawn
     */
    public static void end(LivingEntity entity) {
        TwistPass top = ACTIVE.peek();

        if (top != null && top.getEntity() == entity) {
            ACTIVE.pop();
            top.restore();
        }
    }

    /**
     * Puts the untwisted pose back before EMF animates the model, so EMF reads the vanilla pose.
     *
     * @param body the body part of the model EMF animates, may be null
     */
    public static void untwistTop(ModelPart body) {
        TwistPass top = ACTIVE.peek();

        if (top != null && top.isFor(body))
            top.untwist();
    }

    /**
     * re-applies the twists another mod (EMF) has posed after the twist was applied.
     *
     * @param body        the body part
     * @param rightArm    the right arm part
     * @param leftArm     the left arm part
     * @param head        the head part, may be null
     * @param jacket      the jacket layer, may be null
     * @param rightSleeve the right sleeve layer, may be null
     * @param leftSleeve  the left sleeve layer, may be null
     * @param hat         the hat layer, may be null
     */
    public static void reapplyOn(ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart head,
                                 ModelPart jacket, ModelPart rightSleeve, ModelPart leftSleeve, ModelPart hat) {
        TwistPass top = ACTIVE.peek();

        if (top != null && body != null && rightArm != null && leftArm != null) {
            top.reapplyOn(body, rightArm, leftArm, head, jacket, rightSleeve, leftSleeve, hat);
        }
    }
}

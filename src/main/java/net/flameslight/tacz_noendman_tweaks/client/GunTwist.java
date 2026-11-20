package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.types.upperbodytwist.GunTurnEntry;
import net.flameslight.tacz_noendman_tweaks.types.upperbodytwist.TwistPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The upper body gun twist, applied to the player model after it is set up. When EMF animates the model, the twist
 * steps aside during EMF's animation and is applied again on its result. Render thread only.
 */
public final class GunTwist {

    /*
        Players currently being drawn.
     */
    private static final Deque<GunTurnEntry> ACTIVE = new ArrayDeque<>();

    /**
     * Applies the gun twist to a player model that was just set up, and records the render for the EMF hooks.
     *
     * @param player      the player being drawn
     * @param model       the player's model, already set up for this frame
     * @param partialTick the partial tick of this frame
     */
    public static void begin(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        if (!PlayerAnimatorCompat.LOADED)
            return;

        Minecraft minecraft = Minecraft.getInstance();

        if ((player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson())
                || !TurnMathUtils.isGunTurnEnabled(player)
                || !PlayerAnimatorCompat.isTaczGunPose(player)) {
            ACTIVE.push(new GunTurnEntry(player, model.body, null));
            return;
        }

        float yaw = TurnMathUtils.gunTurnDegrees(player, partialTick) * Mth.DEG_TO_RAD;
        float headTarget = TurnMathUtils.headTargetDegrees(player, partialTick) * Mth.DEG_TO_RAD;

        // The whole gun turn kept (1): torso and arms turn by the gun turn around the model center.
        TwistPass pass = new TwistPass(player, yaw, yaw, 1.0F, yaw, headTarget);
        pass.reapplyOn(model.body, model.rightArm, model.leftArm, model.head,
                model.jacket, model.rightSleeve, model.leftSleeve, model.hat);
        ACTIVE.push(new GunTurnEntry(player, model.body, pass));
    }

    /**
     * Puts the untwisted pose back before EMF animates the model, so EMF reads the vanilla pose.
     *
     * @param body the body part of the model EMF animates, may be null
     */
    public static void beforeAnimation(ModelPart body) {
        GunTurnEntry top = ACTIVE.peek();

        if (top != null && top.isFor(body))
            top.untwist();
    }

    /**
     * Applies the gun twist again on EMF's animation result.
     *
     * @param body the body part of the model EMF animated, may be null
     */
    public static void afterAnimation(ModelPart body) {
        GunTurnEntry top = ACTIVE.peek();

        if (top != null && top.isFor(body) && top.reapply())
            ModLogger.debugOnce("gun turn applied after an EMF animation");
    }

    /**
     * Turns again the parts EMF overwrote with a stored pose.
     *
     * @param body the body part of the model the pose was copied to, may be null
     */
    public static void afterPoseCopy(ModelPart body) {
        GunTurnEntry top = ACTIVE.peek();

        if (top != null && top.isFor(body))
            top.reapply();
    }

    /**
     * Restores the original pose after the entity was drawn.
     *
     * @param entity the entity that was drawn
     */
    public static void end(LivingEntity entity) {
        GunTurnEntry top = ACTIVE.peek();

        if (top != null && top.getEntity() == entity) {
            ACTIVE.pop();
            top.restore();
        }
    }
}

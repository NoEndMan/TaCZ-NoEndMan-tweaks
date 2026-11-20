package net.flameslight.tacz_noendman_tweaks.compat.emf;

import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.types.upperbodytwist.GunTurnEntry;
import net.flameslight.tacz_noendman_tweaks.types.upperbodytwist.TwistPass;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Applies the upper body gun twist, when a player animation resource pack animates the player, after EMF's animation
 * instead of inside Player Animator. Render thread only.
 */
public final class EmfGunTwist {

    private static final boolean ACTIVE_MODS = EmfCompat.LOADED && PlayerAnimatorCompat.LOADED;

    /*
        Players currently being drawn.
     */
    private static final Deque<GunTurnEntry> ACTIVE = new ArrayDeque<>();

    /**
     * Records a player whose model was just set up, for the EMF hook.
     *
     * @param player      the player being drawn
     * @param model       the player's model, already set up for this frame
     * @param partialTick the partial tick of this frame
     */
    public static void begin(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        if (!ACTIVE_MODS)
            return;

        if (!EmfCompat.isTurnDeferred(player)
                || !TurnMathUtils.isGunTurnEnabled(player)
                || !PlayerAnimatorCompat.isTaczGunPose(player)) {
            // No gun turn after EMF, only track whether EMF animates this player.
            ACTIVE.push(new GunTurnEntry(player, model.body, null));
            return;
        }

        float yaw = TurnMathUtils.gunTurnDegrees(player, partialTick) * Mth.DEG_TO_RAD;
        float headTarget = TurnMathUtils.headTargetDegrees(player, partialTick) * Mth.DEG_TO_RAD;

        // The whole gun turn kept (1): torso and arms turn by the gun turn around the model center.
        TwistPass pass = new TwistPass(player, yaw, yaw, 1.0F, yaw, headTarget, true);
        pass.reapplyOn(model.body, model.rightArm, model.leftArm, model.head,
                model.jacket, model.rightSleeve, model.leftSleeve, model.hat);
        ACTIVE.push(new GunTurnEntry(player, model.body, pass));
    }

    /**
     * Marks the player as animated by EMF and applies the gun turn after EMF's animation.
     *
     * @param body the body part of the model EMF animated, may be null
     */
    public static void afterAnimation(ModelPart body) {
        GunTurnEntry top = ACTIVE.peek();

        if (top == null || !top.isFor(body))
            return;

        EmfCompat.markAnimated(top.getEntity());

        if (top.reapply())
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

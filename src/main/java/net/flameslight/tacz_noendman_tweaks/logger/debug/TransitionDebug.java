package net.flameslight.tacz_noendman_tweaks.logger.debug;

import net.flameslight.tacz_noendman_tweaks.client.TwistFade;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Debug logging of every frame of the local player around a gun layer change. Debug mode only.
 */
public final class TransitionDebug {

    /** How many frames before a layer change are kept and written. */
    public static final int BEFORE_FRAMES = 120;

    /** How many ticks after a layer change every frame is written. */
    public static final int AFTER_TICKS = 30;

    /*
        The frames kept before a change, oldest first.
     */
    private static final Deque<String> RECENT = new ArrayDeque<>();

    /*
        The last layer states, the tick up to which frames are written, and the pose after setupAnim.
     */
    private static String lastLayers = "";
    private static int logUntilTick = Integer.MIN_VALUE;
    private static String setupPose = "";

    /**
     * Tells whether frames of this player are logged.
     *
     * @param player the player being drawn
     * @return true in debug mode for the local player
     */
    private static boolean isLogged(AbstractClientPlayer player) {
        return PlayerAnimatorCompat.LOADED && CommonConfig.isDebugMod() && player == Minecraft.getInstance().player;
    }

    /**
     * Records the pose right after setupAnim, before this mod's twists change it.
     *
     * @param player the player being drawn
     * @param model  the player's model
     */
    public static void afterSetupAnim(AbstractClientPlayer player, PlayerModel<?> model) {
        if (isLogged(player)) {
            setupPose = pose(model);
        }
    }

    /**
     * Records the drawn frame and logs it around a gun layer change.
     *
     * @param player      the player that was drawn
     * @param model       the player's model, still holding the drawn pose
     * @param partialTick the partial tick of this frame
     */
    public static void afterRender(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        if (!isLogged(player)) {
            return;
        }
        String layers = PlayerAnimatorCompat.describeGunLayers(player);
        String line = String.format(Locale.ROOT,
                "tick %d + %.2f, %s, riding %s, EMF deferred %s, no-gun strength %.2f, gun turn kept %.2f, head-body %.1f, gun turn %.1f, head target %.1f, body yaw %.1f, head yaw %.1f, whole player rotation %s | after setupAnim %s | drawn %s",
                player.tickCount, partialTick, layers, player.getVehicle() != null, EmfCompat.isTurnDeferred(player),
                TwistFade.strength(player, partialTick), TwistFade.handover(player, partialTick),
                TurnMathUtils.headYaw(player, partialTick),
                TurnMathUtils.gunTurnDegrees(player, partialTick), TurnMathUtils.headTargetDegrees(player, partialTick),
                Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot),
                Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot),
                PlayerAnimatorCompat.describeWholeBodyRotation(player, partialTick), setupPose, pose(model));
        if (!layers.equals(lastLayers)) {
            ModLogger.debug("pose transition: layers changed from [{}] to [{}], the {} frames before:", lastLayers,
                    layers, RECENT.size());
            for (String recent : RECENT) {
                ModLogger.debug("pose transition before: {}", recent);
            }
            RECENT.clear();
            lastLayers = layers;
            logUntilTick = player.tickCount + AFTER_TICKS;
        }
        if (player.tickCount <= logUntilTick) {
            ModLogger.debug("pose transition: {}", line);
        } else {
            RECENT.addLast(line);
            if (RECENT.size() > BEFORE_FRAMES) {
                RECENT.removeFirst();
            }
        }
    }

    /**
     * Formats the pose of the body, head and arms.
     *
     * @param model the player's model
     * @return the pivots and rotations
     */
    private static String pose(PlayerModel<?> model) {
        return "body " + PoseDebug.part(model.body) + ", head " + PoseDebug.part(model.head)
                + ", rightArm " + PoseDebug.part(model.rightArm) + ", leftArm " + PoseDebug.part(model.leftArm);
    }
}

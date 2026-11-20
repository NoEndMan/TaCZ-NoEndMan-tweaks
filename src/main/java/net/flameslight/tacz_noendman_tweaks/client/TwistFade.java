package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.config.TurnSettings;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player fade values updated each client tick
 */
public final class TwistFade {

    /*
        Per player: the strength at the previous tick and at this tick.
     */
    private static final Map<AbstractClientPlayer, float[]> STRENGTH = new WeakHashMap<>();

    /*
        Per player: how much of the gun pose's turn is still kept (1 all, 0 none), at the previous tick and at this
        tick.
     */
    private static final Map<AbstractClientPlayer, float[]> HANDOVER = new WeakHashMap<>();

    /**
     * Updates every player's twist strength and how much of the gun pose's turn is still kept. Called once per
     * client tick.
     */
    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            STRENGTH.clear();
            HANDOVER.clear();
            return;
        }

        for (AbstractClientPlayer player : minecraft.level.players()) {
            updateHandover(player);
            boolean twist = isAllowed(player);
            float[] strength = STRENGTH.get(player);

            if (strength == null) {
                if (!twist) {
                    continue;
                }
                strength = new float[2];
                STRENGTH.put(player, strength);
            }

            strength[0] = strength[1];
            strength[1] = twist
                    ? Math.min(1.0F, strength[1] + TurnSettings.STEP_PER_TICK)
                    : Math.max(0.0F, strength[1] - TurnSettings.STEP_PER_TICK);
        }

        STRENGTH.values().removeIf(strength -> strength[0] == 0.0F && strength[1] == 0.0F);
        HANDOVER.values().removeIf(handover -> handover[0] == 0.0F && handover[1] == 0.0F);

        AbstractClientPlayer local = minecraft.player;

        if (local != null && local.tickCount % 20 == 0 && strength(local, 1.0F) > 0.0F) {
            ModLogger.debug("upper body twist {} degrees (strength {}, head-body {} degrees, riding {})",
                    NoGunTwist.twistDegrees(local, 1.0F) * strength(local, 1.0F), strength(local, 1.0F),
                    TurnMathUtils.headYaw(local, 1.0F), local.getVehicle() != null);
        }
    }

    /**
     * Updates how much of the gun pose's turn, a player still keeps: all of it while holding a gun, then less each
     * tick after the gun is put away.
     *
     * @param player the player
     */
    private static void updateHandover(AbstractClientPlayer player) {
        boolean gun = TurnMathUtils.isGunTurnEnabled(player)
                && PlayerAnimatorCompat.isTaczGunPose(player);
        float[] handover = HANDOVER.get(player);

        if (handover == null) {
            if (!gun)
                return;

            handover = new float[2];
            HANDOVER.put(player, handover);
        }

        float previous = handover[1];

        handover[1] = gun ? 1.0F : Math.max(0.0F, previous - TurnSettings.HANDOVER_STEP_PER_TICK);
        handover[0] = gun ? 1.0F : previous;
    }

    /**
     * Returns how much of the gun pose's turn, the upper body still keeps after a gun was put away, so it eases into
     * the no-gun twist instead of snapping.
     *
     * @param player      the player
     * @param partialTick the partial tick of this frame
     * @return 1 for all of the gun turn, 0 for none of it
     */
    public static float handover(AbstractClientPlayer player, float partialTick) {
        float[] handover = HANDOVER.get(player);
        return handover == null ? 0.0F : Mth.lerp(partialTick, handover[0], handover[1]);
    }

    /**
     * Tells whether the no-gun twist is allowed for a player right now.
     *
     * @param player the player
     * @return true when the twist is allowed
     */
    private static boolean isAllowed(AbstractClientPlayer player) {
        if (!ClientConfig.isWithoutGun())
            return false;

        boolean riding = player.getVehicle() != null;

        if (!ClientConfig.isEnabledFor(riding) || (!riding && !TurnMathUtils.isUpright(player)))
            return false;

        if (isAiming(player) && EmfCompat.isTurnDeferred(player)) {
            // Player animation packs turn the upper body toward the aim themselves.
            return false;
        }

        return !PlayerAnimatorCompat.isAnimating(player);
    }

    /**
     * Tells whether a player aims a bow or crossbow.
     *
     * @param player the player
     * @return true while aiming
     */
    // TODO: works with modded bows, tridents and other items that use aim animation?
    private static boolean isAiming(AbstractClientPlayer player) {
        if (player.isUsingItem()) {
            UseAnim animation = player.getUseItem().getUseAnimation();

            if (animation == UseAnim.BOW || animation == UseAnim.CROSSBOW)
                return true;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack))
                return true;

        }

        return false;
    }

    /**
     * Returns the player's twist strength for this frame.
     *
     * @param player      the player
     * @param partialTick the partial tick of this frame
     * @return the strength from 0 to 1
     */
    public static float strength(AbstractClientPlayer player, float partialTick) {
        float[] strength = STRENGTH.get(player);
        return strength == null ? 0.0F : Mth.lerp(partialTick, strength[0], strength[1]);
    }
}

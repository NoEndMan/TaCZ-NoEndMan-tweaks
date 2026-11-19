package net.flameslight.tacz_noendman_tweaks.client;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.aircraft.AircraftCheck;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCheck;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player strength of the upper body twist, from 0 (off) to 1 (full). Once per client tick it moves toward
 * 1 while the player flies an Immersive Aircraft vehicle with no Player Animator animation playing, and toward
 * 0 otherwise. Rendering interpolates between the last two tick values, so the fade is smooth at any frame rate.
 */
public final class TwistFade {

    /** How much the strength changes per tick: 4 ticks from off to full. */
    public static final float STEP_PER_TICK = 0.25F;

    private static final boolean AIRCRAFT_LOADED = ModList.get().isLoaded(TaczNoEndManTweaks.IMMERSIVE_AIRCRAFT_ID);

    private static final boolean PLAYER_ANIMATOR_LOADED = ModList.get().isLoaded(TaczNoEndManTweaks.PLAYER_ANIMATOR_ID);

    /*
        Per player: index 0 is the strength at the previous tick, index 1 the strength at this tick.
        Weak keys, so players that leave the level are dropped with them.
     */
    private static final Map<AbstractClientPlayer, float[]> STRENGTH = new WeakHashMap<>();

    /**
     * Updates every player's twist strength. Called by ClientEvents at the end of each client tick. Logs the local
     * player's twist once per second in debug mode. Does nothing without Immersive Aircraft.
     */
    public static void onClientTick() {
        if (!AIRCRAFT_LOADED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STRENGTH.clear();
            return;
        }
        for (AbstractClientPlayer player : minecraft.level.players()) {
            boolean twist = AircraftCheck.isFlying(player)
                    && !(PLAYER_ANIMATOR_LOADED && PlayerAnimatorCheck.isAnimating(player));
            float[] strength = STRENGTH.get(player);
            if (strength == null) {
                if (!twist) {
                    continue;
                }
                strength = new float[2];
                STRENGTH.put(player, strength);
            }
            strength[0] = strength[1];
            strength[1] = twist ? Math.min(1.0F, strength[1] + STEP_PER_TICK) : Math.max(0.0F, strength[1] - STEP_PER_TICK);
        }
        STRENGTH.values().removeIf(strength -> strength[0] == 0.0F && strength[1] == 0.0F);

        AbstractClientPlayer local = minecraft.player;
        if (local != null && local.tickCount % 20 == 0 && AircraftCheck.isFlying(local)) {
            ModLogger.debug("upper body twist {} degrees (strength {}, head-body {} degrees, player animator active {})",
                    UpperBodyTwist.twistDegrees(local, 1.0F) * strength(local, 1.0F), strength(local, 1.0F),
                    Mth.wrapDegrees(local.yHeadRot - local.yBodyRot),
                    PLAYER_ANIMATOR_LOADED && PlayerAnimatorCheck.isAnimating(local));
        }
    }

    /**
     * Returns the player's twist strength for this frame.
     *
     * @param player      the player
     * @param partialTick the partial tick of this frame
     * @return the strength from 0 to 1, interpolated between the last two ticks
     */
    public static float strength(AbstractClientPlayer player, float partialTick) {
        float[] strength = STRENGTH.get(player);
        return strength == null ? 0.0F : Mth.lerp(partialTick, strength[0], strength[1]);
    }
}

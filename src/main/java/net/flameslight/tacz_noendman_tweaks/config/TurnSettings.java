package net.flameslight.tacz_noendman_tweaks.config;

/**
 * Fixed upper body twist settings. The configurable ones are in ClientConfig.
 */
public final class TurnSettings {

    /** TACZ's own hard limit, in degrees, for the head-to-body angle it turns the player by. */
    public static final float TACZ_MAX_DEGREES = 85.0F;

    /** Vanilla's own hard limit, in degrees, for the head-to-body angle while riding a living mount. */
    public static final float MOUNT_HEAD_MAX_DEGREES = 85.0F;

    /** Share of the head-to-body angle the torso takes in the no-gun twist. */
    public static final float TORSO_SHARE = 0.6F;

    /** How much the no-gun twist strength changes per tick: 4 ticks from off to full. */
    public static final float STEP_PER_TICK = 0.25F;

    /**
     * How fast the upper body lets go of the gun pose's turn after the gun is put away: 6 ticks from all of it to
     * none of it.
     */
    public static final float HANDOVER_STEP_PER_TICK = 1.0F / 6.0F;
}

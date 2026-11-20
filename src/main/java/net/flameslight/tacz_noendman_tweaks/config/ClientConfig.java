package net.flameslight.tacz_noendman_tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/**
 * The mod's client config: the third person upper body twist options, with cached values.
 */
public final class ClientConfig {

    /** Default of enableOnVehicle. */
    public static final boolean DEFAULT_ON_VEHICLE = true;

    /** Default of onVehicleMaxTurnDegrees. */
    public static final double DEFAULT_ON_VEHICLE_MAX = TurnSettings.MOUNT_HEAD_MAX_DEGREES;

    /** Default of enableInGeneral. */
    public static final boolean DEFAULT_IN_GENERAL = true;

    /** Default of generalMaxTurnDegrees. */
    public static final double DEFAULT_GENERAL_MAX = 55.0;

    /** Default of enableUpperBodyTurnWithoutGun. */
    public static final boolean DEFAULT_WITHOUT_GUN = true;

    /** Default of enableBowAimFix. */
    public static final boolean DEFAULT_BOW_AIM_FIX = true;

    /** Default of enableTridentAimFix. */
    public static final boolean DEFAULT_TRIDENT_AIM_FIX = true;

    /** The registered client config spec. */
    public static final ForgeConfigSpec SPEC;
    /** enableOnVehicle option. */
    public static final ForgeConfigSpec.BooleanValue ON_VEHICLE;
    /** onVehicleMaxTurnDegrees option. */
    public static final ForgeConfigSpec.DoubleValue ON_VEHICLE_MAX;
    /** enableInGeneral option. */
    public static final ForgeConfigSpec.BooleanValue IN_GENERAL;
    /** generalMaxTurnDegrees option. */
    public static final ForgeConfigSpec.DoubleValue GENERAL_MAX;
    /** enableUpperBodyTurnWithoutGun option. */
    public static final ForgeConfigSpec.BooleanValue WITHOUT_GUN;
    /** enableBowAimFix option. */
    public static final ForgeConfigSpec.BooleanValue BOW_AIM_FIX;
    /** enableTridentAimFix option. */
    public static final ForgeConfigSpec.BooleanValue TRIDENT_AIM_FIX;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Third person upper body twist toward where the player looks.")
                .push("upper_body_twist_tweak");

        ON_VEHICLE = builder
                .comment("While riding any vehicle or mount (boats, horses, Immersive Aircraft and others), turns the upper body",
                        "toward where the player looks. Past 85 degrees on vehicles, the whole body turns by the rest of the angle.")
                .define("enableOnVehicle", DEFAULT_ON_VEHICLE);

        ON_VEHICLE_MAX = builder
                .comment("Largest angle, in degrees, the upper body turns while riding.")
                .defineInRange("onVehicleMaxTurnDegrees", DEFAULT_ON_VEHICLE_MAX, 0.0, TurnSettings.MOUNT_HEAD_MAX_DEGREES);

        IN_GENERAL = builder
                .comment("While standing or crouching, turns only the upper body toward where the player looks. With a TACZ gun, past",
                        "generalMaxTurnDegrees the whole body turns by the rest of the angle, instead of TACZ turning the whole body.")
                .define("enableInGeneral", DEFAULT_IN_GENERAL);

        GENERAL_MAX = builder
                .comment("Largest angle, in degrees, the upper body turns away from the legs while on foot.")
                .defineInRange("generalMaxTurnDegrees", DEFAULT_GENERAL_MAX, 0.0, TurnSettings.TACZ_MAX_DEGREES);

        WITHOUT_GUN = builder
                .comment("Also twists the torso toward the head while not holding a TACZ gun, on a vehicle or on foot as allowed above.",
                        "The arms keep their own rotation. The torso takes 60 percent of the head-to-body angle, up to the matching max turn.")
                .define("enableUpperBodyTurnWithoutGun", DEFAULT_WITHOUT_GUN);

        builder.pop();

        builder.comment("EMF player animation fixes tweaks.")
                .push("emf_player_animation_fixes_tweak");

        BOW_AIM_FIX = builder
                .comment("With EMF, pauses the no-gun torso twist while aiming a bow or crossbow, so player animation packs",
                        "(like Player Extension) can pose the aim themselves.")
                .define("enableBowAimFix", DEFAULT_BOW_AIM_FIX);

        TRIDENT_AIM_FIX = builder
                .comment("Turns the aimed trident toward where the player looks, on foot and riding. With EMF, also removes",
                        "vanilla's riding arm offset from the trident aiming pose, so player animation packs detect it while riding.")
                .define("enableTridentAimFix", DEFAULT_TRIDENT_AIM_FIX);

        builder.pop();
        SPEC = builder.build();
    }

    private static volatile boolean resolved = false;
    private static volatile boolean onVehicle = DEFAULT_ON_VEHICLE;
    private static volatile float onVehicleMax = (float) DEFAULT_ON_VEHICLE_MAX;
    private static volatile boolean inGeneral = DEFAULT_IN_GENERAL;
    private static volatile float generalMax = (float) DEFAULT_GENERAL_MAX;
    private static volatile boolean withoutGun = DEFAULT_WITHOUT_GUN;
    private static volatile boolean bowAimFix = DEFAULT_BOW_AIM_FIX;
    private static volatile boolean tridentAimFix = DEFAULT_TRIDENT_AIM_FIX;

    /**
     * Drops the cached values so they are read again.
     */
    public static void refresh() {
        resolved = false;
    }

    /**
     * Clears the cache when this config loads or reloads.
     *
     * @param config    the config of the event
     * @param unloading true if the config is unloading
     */
    public static void onConfigLoaded(ModConfig config, boolean unloading) {
        if (!unloading && config.getSpec() == SPEC)
            refresh();
    }

    /**
     * Reads every option into the cache.
     *
     * @return false while the config is not loaded yet
     */
    private static boolean resolve() {
        if (resolved)
            return true;

        if (!SPEC.isLoaded())
            return false;

        onVehicle = ON_VEHICLE.get();
        onVehicleMax = ON_VEHICLE_MAX.get().floatValue();
        inGeneral = IN_GENERAL.get();
        generalMax = GENERAL_MAX.get().floatValue();
        withoutGun = WITHOUT_GUN.get();
        bowAimFix = BOW_AIM_FIX.get();
        tridentAimFix = TRIDENT_AIM_FIX.get();
        resolved = true;
        return true;
    }

    /**
     * Reads enableOnVehicle. Like every getter here, returns the default until the config has loaded.
     *
     * @return true when the upper body turns while riding
     */
    public static boolean isOnVehicle() {
        return resolve() ? onVehicle : DEFAULT_ON_VEHICLE;
    }

    /**
     * Reads onVehicleMaxTurnDegrees.
     *
     * @return the largest upper body turn while riding, in degrees
     */
    public static float onVehicleMaxTurnDegrees() {
        return resolve() ? onVehicleMax : (float) DEFAULT_ON_VEHICLE_MAX;
    }

    /**
     * Reads enableInGeneral.
     *
     * @return true when the upper body turns while on foot
     */
    public static boolean isInGeneral() {
        return resolve() ? inGeneral : DEFAULT_IN_GENERAL;
    }

    /**
     * Reads generalMaxTurnDegrees.
     *
     * @return the largest upper body turn away from the legs while on foot, in degrees
     */
    public static float generalMaxTurnDegrees() {
        return resolve() ? generalMax : (float) DEFAULT_GENERAL_MAX;
    }

    /**
     * Reads enableUpperBodyTurnWithoutGun.
     *
     * @return true when the torso also twists while not holding a TACZ gun
     */
    public static boolean isWithoutGun() {
        return resolve() ? withoutGun : DEFAULT_WITHOUT_GUN;
    }

    /**
     * Reads enableBowAimFix.
     *
     * @return true when the no-gun twist pauses while aiming a bow or crossbow with an EMF-animated model
     */
    public static boolean isBowAimFix() {
        return resolve() ? bowAimFix : DEFAULT_BOW_AIM_FIX;
    }

    /**
     * Reads enableTridentAimFix.
     *
     * @return true when the trident aiming pose is fixed
     */
    public static boolean isTridentAimFix() {
        return resolve() ? tridentAimFix : DEFAULT_TRIDENT_AIM_FIX;
    }

    /**
     * Tells whether the upper body turn is on for a player's current situation, riding or on foot.
     *
     * @param riding true when the player has a vehicle
     * @return enableOnVehicle while riding, enableInGeneral otherwise
     */
    public static boolean isEnabledFor(boolean riding) {
        return riding ? isOnVehicle() : isInGeneral();
    }

    /**
     * Returns the max turn for a player's current situation, riding or on foot.
     *
     * @param riding true when the player has a vehicle
     * @return onVehicleMaxTurnDegrees while riding, generalMaxTurnDegrees otherwise, in degrees
     */
    public static float maxTurnDegreesFor(boolean riding) {
        return riding ? onVehicleMaxTurnDegrees() : generalMaxTurnDegrees();
    }
}

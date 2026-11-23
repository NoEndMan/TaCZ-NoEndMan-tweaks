package net.flameslight.tacz_noendman_tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The mod's client config: the third person upper body twist options, the EMF player animation fixes and the TaCZ
 * tweaks.
 */
public final class ClientConfig {

    /**
     * Default of enableOnVehicle.
     */
    public static final boolean DEFAULT_ON_VEHICLE = true;

    /**
     * Default of onVehicleMaxTurnDegrees.
     */
    public static final double DEFAULT_ON_VEHICLE_MAX = TurnSettings.MOUNT_HEAD_MAX_DEGREES;

    /**
     * Default of enableOutsideVehicle.
     */
    public static final boolean DEFAULT_OUTSIDE_VEHICLE = true;

    /**
     * Default of outsideVehicleMaxTurnDegrees.
     */
    public static final double DEFAULT_OUTSIDE_VEHICLE_MAX = 55.0;

    /**
     * Default of enableTurnWithGunFix.
     */
    public static final boolean DEFAULT_TURN_WITH_GUN_FIX = true;

    /**
     * Default of enableBowAimFix.
     */
    public static final boolean DEFAULT_BOW_AIM_FIX = true;

    /**
     * Default of enableTridentAimFix.
     */
    public static final boolean DEFAULT_TRIDENT_AIM_FIX = true;

    /**
     * Default of enableFirstEquipWarmUp.
     */
    public static final boolean DEFAULT_FIRST_EQUIP_WARM_UP = true;

    /**
     * The registered client config spec.
     */
    public static final ForgeConfigSpec SPEC;
    /**
     * enableOnVehicle option.
     */
    public static final ForgeConfigSpec.BooleanValue ON_VEHICLE;
    /**
     * onVehicleMaxTurnDegrees option.
     */
    public static final ForgeConfigSpec.DoubleValue ON_VEHICLE_MAX;
    /**
     * enableOutsideVehicle option.
     */
    public static final ForgeConfigSpec.BooleanValue OUTSIDE_VEHICLE;
    /**
     * outsideVehicleMaxTurnDegrees option.
     */
    public static final ForgeConfigSpec.DoubleValue OUTSIDE_VEHICLE_MAX;
    /**
     * enableTurnWithGunFix option.
     */
    public static final ForgeConfigSpec.BooleanValue TURN_WITH_GUN_FIX;
    /**
     * enableBowAimFix option.
     */
    public static final ForgeConfigSpec.BooleanValue BOW_AIM_FIX;
    /**
     * enableTridentAimFix option.
     */
    public static final ForgeConfigSpec.BooleanValue TRIDENT_AIM_FIX;
    /**
     * enableFirstEquipWarmUp option.
     */
    public static final ForgeConfigSpec.BooleanValue FIRST_EQUIP_WARM_UP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Third person upper-body twist toward where the player looks.")
                .push("upper_body_twist_tweak");

        ON_VEHICLE = builder
                .comment("While riding any vehicle or mount (boats, horses, Immersive Aircraft and others), turns the upper body",
                        "toward where the player looks. Past onVehicleMaxTurnDegrees degrees on vehicles/mounts, the whole body",
                        "turns by the rest of the angle.",
                        "Default value: " + DEFAULT_ON_VEHICLE)
                .define("enableOnVehicle", DEFAULT_ON_VEHICLE);

        ON_VEHICLE_MAX = builder
                .comment("Largest angle, in degrees, the upper body turns while riding.",
                        "Default value: " + DEFAULT_ON_VEHICLE_MAX)
                .defineInRange("onVehicleMaxTurnDegrees", DEFAULT_ON_VEHICLE_MAX, 0.0, TurnSettings.MOUNT_HEAD_MAX_DEGREES);

        OUTSIDE_VEHICLE = builder
                .comment("On foot (standing, walking, running, crouching and creative flying), turns the upper body toward ",
                        "where the player looks. Without a gun, the torso takes 60 percent of the head-to-body ",
                        "angle and the arms keep their own rotation.",
                        "Default value: " + DEFAULT_OUTSIDE_VEHICLE)
                .define("enableOutsideVehicle", DEFAULT_OUTSIDE_VEHICLE);

        OUTSIDE_VEHICLE_MAX = builder
                .comment("Largest angle, in degrees, the upper body turns away from the legs while on foot.",
                        "Default value: " + DEFAULT_OUTSIDE_VEHICLE_MAX)
                .defineInRange("outsideVehicleMaxTurnDegrees", DEFAULT_OUTSIDE_VEHICLE_MAX, 0.0, TurnSettings.TACZ_MAX_DEGREES);

        TURN_WITH_GUN_FIX = builder
                .comment("Requires TaCZ installed on client side. If enabled, turning with guns would follow upper body twist rules,",
                        "instead of TaCZ turning the whole body. When disabled, TaCZ's own gun pose turning is unchanged.",
                        "Default value: " + DEFAULT_TURN_WITH_GUN_FIX)
                .define("enableTurnWithGunFix", DEFAULT_TURN_WITH_GUN_FIX);

        builder.pop();

        builder.comment("Player animation fixes tweaks.")
                .push("emf_player_animation_fixes_tweak");

        BOW_AIM_FIX = builder
                .comment("When enabled, pauses this mod upper body twist while aiming a bow or crossbow, so player",
                        "animation resource packs, like Player Extension, could pose the aim themselves.",
                        "Default value: " + DEFAULT_BOW_AIM_FIX)
                .define("enableBowAimFix", DEFAULT_BOW_AIM_FIX);

        TRIDENT_AIM_FIX = builder
                .comment("Turns the aimed trident toward where the player looks, on foot and riding. With EMF, also removes",
                        "vanilla's on-vehicle arm offset from the trident aiming pose, so player animation resource pack detect it while riding.",
                        "Default value: " + DEFAULT_TRIDENT_AIM_FIX)
                .define("enableTridentAimFix", DEFAULT_TRIDENT_AIM_FIX);

        builder.pop();

        builder.comment("TaCZ optimizations. Requires TaCZ installed on client side.",
                        "This mod's other TaCZ optimizations do not affect gameplay, so they are always on and not toggleable.")
                .push("tacz_optimizations");

        FIRST_EQUIP_WARM_UP = builder
                .comment("If enabled, lowers the lag spike of the first gun equip per game launch: Once per game launch, after ",
                        "joining a world, prepares TaCZ's Lua script code in the background, then draws the first person arms, ",
                        "one gun model, one attachment of each type and, while a shader pack is active, one third person gun once, ",
                        "all hidden, so the first equip of any gun does not run that code for the first time.",
                        "Costs a few hundred milliseconds spread over the first seconds after joining a world.",
                        "Default value: " + DEFAULT_FIRST_EQUIP_WARM_UP)
                .define("enableFirstEquipWarmUp", DEFAULT_FIRST_EQUIP_WARM_UP);

        builder.pop();
        SPEC = builder.build();
    }

    /**
     * Reads enableOnVehicle. Like every getter here, returns the default until the config has loaded.
     *
     * @return true when the upper body turns while riding
     */
    public static boolean isOnVehicle() {
        return SPEC.isLoaded() ? ON_VEHICLE.get() : DEFAULT_ON_VEHICLE;
    }

    /**
     * Reads onVehicleMaxTurnDegrees.
     *
     * @return the largest upper body turn while riding, in degrees
     */
    public static float onVehicleMaxTurnDegrees() {
        return (float) (SPEC.isLoaded() ? ON_VEHICLE_MAX.get() : DEFAULT_ON_VEHICLE_MAX);
    }

    /**
     * Reads enableOutsideVehicle.
     *
     * @return true when the upper body turns while on foot
     */
    public static boolean isOutsideVehicle() {
        return SPEC.isLoaded() ? OUTSIDE_VEHICLE.get() : DEFAULT_OUTSIDE_VEHICLE;
    }

    /**
     * Reads outsideVehicleMaxTurnDegrees.
     *
     * @return the largest upper body turn away from the legs while on foot, in degrees
     */
    public static float outsideVehicleMaxTurnDegrees() {
        return (float) (SPEC.isLoaded() ? OUTSIDE_VEHICLE_MAX.get() : DEFAULT_OUTSIDE_VEHICLE_MAX);
    }

    /**
     * Reads enableTurnWithGunFix.
     *
     * @return true when the upper body also turns while holding a TaCZ gun
     */
    public static boolean isTurnWithGunFix() {
        return SPEC.isLoaded() ? TURN_WITH_GUN_FIX.get() : DEFAULT_TURN_WITH_GUN_FIX;
    }

    /**
     * Reads enableBowAimFix.
     *
     * @return true when the no-gun twist pauses while aiming a bow or crossbow with an EMF-animated model
     */
    public static boolean isBowAimFix() {
        return SPEC.isLoaded() ? BOW_AIM_FIX.get() : DEFAULT_BOW_AIM_FIX;
    }

    /**
     * Reads enableTridentAimFix.
     *
     * @return true when the trident aiming pose is fixed
     */
    public static boolean isTridentAimFix() {
        return SPEC.isLoaded() ? TRIDENT_AIM_FIX.get() : DEFAULT_TRIDENT_AIM_FIX;
    }

    /**
     * Reads enableFirstEquipWarmUp.
     *
     * @return true when the first gun equip optimizations (Lua warm-up, arm and gun model pre-renders) run
     */
    public static boolean isFirstEquipWarmUp() {
        return SPEC.isLoaded() ? FIRST_EQUIP_WARM_UP.get() : DEFAULT_FIRST_EQUIP_WARM_UP;
    }

    /**
     * Tells whether the upper body turn is on for a player's current situation, riding or on foot.
     *
     * @param riding true when the player has a vehicle
     * @return enableOnVehicle while riding, enableOutsideVehicle otherwise
     */
    public static boolean isEnabledFor(boolean riding) {
        return riding ? isOnVehicle() : isOutsideVehicle();
    }

    /**
     * Returns the max turn for a player's current situation, riding or on foot.
     *
     * @param riding true when the player has a vehicle
     * @return onVehicleMaxTurnDegrees while riding, outsideVehicleMaxTurnDegrees otherwise, in degrees
     */
    public static float maxTurnDegreesFor(boolean riding) {
        return riding ? onVehicleMaxTurnDegrees() : outsideVehicleMaxTurnDegrees();
    }
}

package net.flameslight.tacz_noendman_tweaks.compat.emf;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Entity Model Features compatibility: whether it is installed, whether it animates a player, and safe access to it.
 * EMF own classes are only used in EmfTwistHook.
 */
public final class EmfCompat {

    /** Mod id of Entity Model Features. */
    public static final String MOD_ID = "entity_model_features";

    /** True when EMF is installed. */
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    /** How many ticks after EMF last animated a player the turn stays deferred. */
    public static final int DEFER_TICKS = 2;

    /*
        Per player: the tick at which EMF last animated its model.
     */
    private static final Map<LivingEntity, Integer> LAST_EMF_TICK = new WeakHashMap<>();

    /**
     * Registers this mod's EMF animation hook, when EMF is installed.
     */
    public static void registerHook() {
        if (LOADED)
            EmfTwistHook.register();
    }

    /**
     * Records that EMF just animated the player's model.
     *
     * @param player the player
     */
    public static void markAnimated(LivingEntity player) {
        LAST_EMF_TICK.put(player, player.tickCount);
    }

    /**
     * Tells whether EMF animates the player, so its upper body is turned after EMF's animation.
     *
     * @param player the player
     * @return true when EMF animated the player in the last DEFER_TICKS ticks
     */
    public static boolean isTurnDeferred(LivingEntity player) {
        if (!LOADED)
            return false;

        Integer last = LAST_EMF_TICK.get(player);

        if (last == null)
            return false;

        int age = player.tickCount - last;
        return age >= 0 && age <= DEFER_TICKS;
    }
}

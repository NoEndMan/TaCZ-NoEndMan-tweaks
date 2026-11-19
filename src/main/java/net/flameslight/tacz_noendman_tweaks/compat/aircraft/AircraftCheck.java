package net.flameslight.tacz_noendman_tweaks.compat.aircraft;

import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Immersive Aircraft access. Only loaded when Immersive Aircraft is installed.
 */
public final class AircraftCheck {

    /**
     * Tells whether the player rides an Immersive Aircraft vehicle.
     *
     * @param player the player
     * @return true when the player's vehicle is an Immersive Aircraft vehicle
     */
    public static boolean isFlying(Player player) {
        return player.getVehicle() instanceof VehicleEntity;
    }
}

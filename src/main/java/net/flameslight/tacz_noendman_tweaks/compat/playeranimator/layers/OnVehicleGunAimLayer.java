package net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimationDebug;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorAccessor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Player Animator layer that turns a riding player's upper body toward where the player looks while holding a gun,
 * on any vehicle.
 */
public final class OnVehicleGunAimLayer implements IAnimation {

    /** Id of this layer in Player Animator's per-player data. */
    public static final ResourceLocation ID = new ResourceLocation(TaczNoEndManTweaks.MOD_ID, "riding_gun_aim");

    /** Layer priority, applied after TACZ's layers. */
    public static final int PRIORITY = 97;

    private static final String LOG_NAME = "riding gun aim layer";

    private final AbstractClientPlayer player;

    /**
     * Creates the layer of one player.
     *
     * @param player the player
     */
    private OnVehicleGunAimLayer(AbstractClientPlayer player) {
        this.player = player;
    }

    /**
     * Registers the layer with Player Animator.
     */
    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ID, PRIORITY, OnVehicleGunAimLayer::new);
    }

    /**
     * Tells whether the layer is active: riding with a gun and enableOnVehicle on.
     *
     * @return true when active
     */
    @Override
    public boolean isActive() {
        return this.player.getVehicle() != null && TurnMathUtils.isGunTurnEnabled(this.player)
                && PlayerAnimatorCompat.isTaczGunPose(this.player);
    }

    @Override
    public void setupAnim(float tickDelta) {}

    /**
     * Turns the torso, head and arms toward where the player looks. With EMF, GunTorsoTurn does it instead.
     *
     * @param modelName the Player Animator part name
     * @param type      the transform type
     * @param tickDelta the partial tick
     * @param value     the value the lower layers produced
     * @return the turned value
     */
    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value) {
        if (type == TransformType.BEND
                || !PlayerAnimatorAccessor.isUpperBody(modelName)
                || EmfCompat.isTurnDeferred(this.player))
            return value;

        float yaw = TurnMathUtils.gunTurnDegrees(this.player, tickDelta) * Mth.DEG_TO_RAD;

        if (yaw == 0.0F)
            return value;

        Vec3f turned = PlayerAnimatorAccessor.turnPart(this.player, modelName, type, value, yaw, tickDelta);
        PlayerAnimationDebug.logState(LOG_NAME, this.player, tickDelta, yaw * Mth.RAD_TO_DEG, 0.0F);
        PlayerAnimationDebug.logTransform(LOG_NAME, this.player, modelName, type, yaw, value, turned);

        return turned;
    }
}

package net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.config.TurnSettings;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Player Animator layer that, on foot in an upright pose with a gun, keeps only the part of TACZ's whole-player turn
 * past the twist limit, so the legs follow only once the upper body twist (EmfGunTwist) reached its limit.
 */
public final class OutsideVehicleGunTwistLayer implements IAnimation {

    /** Id of this layer in Player Animator's per-player data. */
    public static final ResourceLocation ID = new ResourceLocation(TaczNoEndManTweaks.MOD_ID, "gun_upper_body_twist");

    /** Layer priority, applied after TACZ's layers. */
    public static final int PRIORITY = 97;

    private final AbstractClientPlayer player;

    /**
     * Creates the layer of one player.
     *
     * @param player the player
     */
    private OutsideVehicleGunTwistLayer(AbstractClientPlayer player) {
        this.player = player;
    }

    /**
     * Registers the layer with Player Animator.
     */
    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ID, PRIORITY, OutsideVehicleGunTwistLayer::new);
    }

    /**
     * Tells whether the layer is active: on foot with a gun, with enableTurnWithGunFix and enableOutsideVehicle on.
     *
     * @return true when active
     */
    @Override
    public boolean isActive() {
        return this.player.getVehicle() == null && TurnMathUtils.isGunTurnEnabled(this.player)
                && PlayerAnimatorCompat.isTaczGunPose(this.player);
    }

    @Override
    public void setupAnim(float tickDelta) {}

    /**
     * Keeps only the part of TACZ's whole player turn past the twist limit. EmfGunTwist turns the upper body by the
     * rest.
     *
     * @param modelName the Player Animator part name
     * @param type      the transform type
     * @param tickDelta the partial tick
     * @param value     the value the lower layers produced
     * @return the changed value
     */
    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value) {
        if (type != TransformType.ROTATION || !"body".equals(modelName))
            return value;

        float taczYaw = Mth.clamp(TurnMathUtils.headYaw(this.player, tickDelta),
                -TurnSettings.TACZ_MAX_DEGREES, TurnSettings.TACZ_MAX_DEGREES);
        float twist = TurnMathUtils.gunTurnDegrees(this.player, tickDelta);

        if (twist == 0.0F) {
            TurnMathUtils.setWholeBodyYaw(this.player, value.getY());
            return value;
        }

        // TACZ turned the whole player by taczYaw. Keep only the share past the twist limit.
        float kept = (taczYaw - twist) / taczYaw;
        Vec3f changed = new Vec3f(value.getX(), value.getY() * kept, value.getZ());
        TurnMathUtils.setWholeBodyYaw(this.player, changed.getY());
        return changed;
    }
}

package net.flameslight.tacz_noendman_tweaks.compat.playeranimator.layers;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.config.TurnSettings;
import net.flameslight.tacz_noendman_tweaks.util.TurnMathUtils;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimationDebug;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorAccessor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Player Animator layer that, on foot with a gun, twists only the upper body toward where the player looks instead
 * of TACZ turning the whole player. The legs follow once the twist limit is reached.
 */
public final class OutsideVehicleGunTwistLayer implements IAnimation {

    /** Id of this layer in Player Animator's per-player data. */
    public static final ResourceLocation ID = new ResourceLocation(TaczNoEndManTweaks.MOD_ID, "gun_upper_body_twist");

    /** Layer priority, applied after TACZ's layers. */
    public static final int PRIORITY = 97;

    private static final String LOG_NAME = "gun upper body twist layer";

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
     * Tells whether the layer is active: on foot with a gun and enableInGeneral on.
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
     * Splits TACZ's whole player turn into an upper body twist and the rest on the whole player. With EMF,
     * GunTorsoTurn applies the upper body twist instead.
     *
     * @param modelName the Player Animator part name
     * @param type      the transform type
     * @param tickDelta the partial tick
     * @param value     the value the lower layers produced
     * @return the changed value
     */
    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value) {
        boolean wholeBody = "body".equals(modelName);

        if (type == TransformType.BEND
                || !(wholeBody || PlayerAnimatorAccessor.isUpperBody(modelName))
                || (wholeBody && type != TransformType.ROTATION)) {
            return value;
        }

        float taczYaw = Mth.clamp(TurnMathUtils.headYaw(this.player, tickDelta),
                -TurnSettings.TACZ_MAX_DEGREES, TurnSettings.TACZ_MAX_DEGREES);
        float twist = TurnMathUtils.gunTurnDegrees(this.player, tickDelta);

        if (twist == 0.0F) {
            if (wholeBody)
                TurnMathUtils.setWholeBodyYaw(this.player, value.getY());

            return value;
        }

        float twistRadians = twist * Mth.DEG_TO_RAD;
        Vec3f changed;

        if (wholeBody) {
            // TACZ turned the whole player by taczYaw. Keep only the share past the twist limit.
            float kept = (taczYaw - twist) / taczYaw;
            changed = new Vec3f(value.getX(), value.getY() * kept, value.getZ());
            TurnMathUtils.setWholeBodyYaw(this.player, changed.getY());
        } else if (EmfCompat.isTurnDeferred(this.player)) {
            // EMF animates this player: GunTorsoTurn turns the upper body after EMF's animation instead.
            return value;
        } else {
            changed = PlayerAnimatorAccessor.turnPart(this.player, modelName, type, value, twistRadians, tickDelta);
        }

        PlayerAnimationDebug.logState(LOG_NAME, this.player, tickDelta, twist, taczYaw - twist);
        PlayerAnimationDebug.logTransform(LOG_NAME, this.player, modelName, type, twistRadians, value, changed);

        return changed;
    }
}

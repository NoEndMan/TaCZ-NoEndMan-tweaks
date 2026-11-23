package net.flameslight.tacz_noendman_tweaks.mixin.tacz;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.entity.shooter.LivingEntitySpeedModifier;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * TaCZ's updateSpeedModifier calls isAlive first, every tick, for every living entity. For an entity
 * that holds no gun, the only thing the method can do is remove TaCZ's two speed modifiers, and removing a
 * modifier that is not there, does nothing. So for those entities the cheap checks run first and the method
 * returns without calling isAlive when neither modifier is present. Gun holders, and entities that still carry
 * a TaCZ modifier, run TaCZ's method unchanged.
 */
@Mixin(value = LivingEntitySpeedModifier.class, remap = false)
public abstract class LivingEntitySpeedModifierMixin {

    @Shadow
    @Final
    private static UUID EXTRA_SPEED_MODIFIER_UUID;

    @Shadow
    @Final
    private static UUID WEIGHT_SPEED_MODIFIER_UUID;

    @Shadow
    @Final
    private LivingEntity shooter;

    /**
     * Returns early for entities without a gun and without TaCZ speed modifiers.
     *
     * @param ci the callback info, cancelled when TaCZ's method would change nothing
     */
    @Inject(method = "updateSpeedModifier", at = @At("HEAD"), cancellable = true)
    private void tacz_noendman$skipWhenNothingToRemove(CallbackInfo ci) {
        if (this.shooter.getMainHandItem().getItem() instanceof AbstractGunItem) {
            return;
        }
        AttributeInstance speed = this.shooter.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null
                || (speed.getModifier(WEIGHT_SPEED_MODIFIER_UUID) == null && speed.getModifier(EXTRA_SPEED_MODIFIER_UUID) == null)) {
            ModLogger.debugOnce("fix active: speed modifier check order");
            ci.cancel();
        }
    }
}

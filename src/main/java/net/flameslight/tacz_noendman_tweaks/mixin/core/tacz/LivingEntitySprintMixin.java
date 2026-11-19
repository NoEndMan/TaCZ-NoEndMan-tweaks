package net.flameslight.tacz_noendman_tweaks.mixin.core.tacz;

import com.tacz.guns.entity.shooter.LivingEntitySprint;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fix 4. TACZ's getProcessedSprintStatus returns either false or the sprint value it was given.
 * When that value is already false the answer is false whatever the aim and reload state are,
 * so the two synced data reads are skipped.
 */
@Mixin(value = LivingEntitySprint.class, remap = false)
public abstract class LivingEntitySprintMixin {

    /**
     * Returns false at once when the sprint value passed in is false.
     *
     * @param sprinting the sprint value TACZ was given
     * @param cir       the callback, set to false when sprinting is false
     */
    @Inject(method = "getProcessedSprintStatus", at = @At("HEAD"), cancellable = true)
    private void tacz_noendman$skipWhenNotSprinting(boolean sprinting, CallbackInfoReturnable<Boolean> cir) {
        if (!sprinting) {
            ModLogger.debugOnce("fix active: sprint check short-cut");
            cir.setReturnValue(false);
        }
    }
}

package net.flameslight.tacz_noendman_tweaks.mixin.tacz;

import com.tacz.guns.util.CycleTaskHelper;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * TaCZ's CycleTaskHelper.tick copies its task list into a second list, clears it, filters it and copies
 * it back every tick. With an empty task list every one of those steps changes nothing, so this code
 * makes tacz skips this expensive operations.
 * The second list is always empty between ticks, because tick clears it at the end.
 */
@Mixin(value = CycleTaskHelper.class, remap = false)
public abstract class CycleTaskHelperMixin {

    @Shadow
    @Final
    private static List<?> CYCLE_TASKS;

    /**
     * Skips the tick when no task is queued.
     *
     * @param ci the callback info, cancelled when the task list is empty
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void tacz_noendman$skipWhenEmpty(CallbackInfo ci) {
        if (CYCLE_TASKS.isEmpty()) {
            ModLogger.debugOnce("fix active: empty task list");
            ci.cancel();
        }
    }
}

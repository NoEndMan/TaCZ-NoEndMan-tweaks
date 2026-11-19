package net.flameslight.tacz_noendman_tweaks.mixin.core.tacz;

import com.tacz.guns.entity.sync.core.DataHolder;
import com.tacz.guns.entity.sync.core.DataHolderCapabilityProvider;
import com.tacz.guns.entity.sync.core.SyncedDataKey;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.types.DataHolderCache;
import net.flameslight.tacz_noendman_tweaks.types.RegisteredKeyFlag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Fast paths in TACZ's synced entity data registry. Both give exactly the results of TACZ's own code.
 * <ul>
 *     <li>Fix 2: the registration check in get and set skips the hash set lookup for key objects
 *     marked as registered. Unmarked keys still go through the original check.</li>
 *     <li>Fix 3: getDataHolder reuses the entity's cached capability result instead of scanning all
 *     attached capabilities. The cache is refilled whenever it is empty or no longer present, and cleared
 *     whenever Forge invalidates or revives the entity's capabilities.</li>
 * </ul>
 */
@Mixin(value = SyncedEntityData.class, remap = false)
public abstract class SyncedEntityDataMixin {

    @Shadow
    @Final
    private Set<SyncedDataKey<?, ?>> registeredDataKeys;

    /**
     * Marks a key as registered once TACZ accepted it. TACZ throws before returning when it rejects a key,
     * so reaching RETURN means the key is now in registeredDataKeys, and keys are never removed.
     *
     * @param key the key that was registered
     * @param ci  the callback info
     */
    @Inject(method = "registerDataKey", at = @At("RETURN"))
    private void tacz_noendman$markRegistered(SyncedDataKey<?, ?> key, CallbackInfo ci) {
        ((RegisteredKeyFlag) (Object) key).tacz_noendman$markRegistered();
    }

    /**
     * Replaces the registration check in get and set. A marked key is known to be in the set.
     * Any other call keeps TACZ's original lookup.
     *
     * @param set the set TACZ checks
     * @param key the key TACZ looks for
     * @return whether the set contains the key, the same answer as TACZ's own check
     */
    @Redirect(method = {"get", "set"}, at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"))
    private boolean tacz_noendman$fastRegistrationCheck(Set<?> set, Object key) {
        if (set == this.registeredDataKeys && key instanceof RegisteredKeyFlag flag && flag.tacz_noendman$isRegistered()) {
            ModLogger.debugOnce("fix active: synced data registration check");
            ModLogger.debugVerify("synced data registration check", () -> set.contains(key), Boolean.TRUE);
            return true;
        }
        return set.contains(key);
    }

    /**
     * Returns the entity's DataHolder from its cached capability result when that result is still present.
     * Otherwise does the original lookup and caches it. The original code is
     * entity.getCapability(CAPABILITY, null).resolve().orElse(null), and LazyOptional.orElse(null) gives the
     * same value without allocating an Optional.
     *
     * @param entity the entity
     * @param cir    the callback, set to the holder or null
     */
    @Inject(method = "getDataHolder", at = @At("HEAD"), cancellable = true)
    private void tacz_noendman$cachedDataHolder(Entity entity, CallbackInfoReturnable<DataHolder> cir) {
        DataHolderCache cache = (DataHolderCache) entity;
        LazyOptional<DataHolder> holder = cache.tacz_noendman$getCachedHolder();
        if (holder == null || !holder.isPresent()) {
            holder = entity.getCapability(DataHolderCapabilityProvider.CAPABILITY, null);
            cache.tacz_noendman$setCachedHolder(holder.isPresent() ? holder : null);
        }
        DataHolder result = holder.orElse(null);
        ModLogger.debugOnce("fix active: data holder cache");
        ModLogger.debugVerify("data holder cache",
                () -> entity.getCapability(DataHolderCapabilityProvider.CAPABILITY, null).resolve().orElse(null), result);
        cir.setReturnValue(result);
    }
}

package net.flameslight.tacz_noendman_tweaks.mixin.tacz.client;

import com.tacz.guns.client.resource.GunDisplayInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads whether TaCZ finished loading a gun's model, without waiting for it (TaCZ's getGunModel blocks until the model
 * is loaded). Used by the first equip pre-render.
 */
@Mixin(value = GunDisplayInstance.class, remap = false)
public interface GunDisplayInstanceAccessor {

    /**
     * Reads TaCZ's volatile modelLoaded flag.
     *
     * @return true when the gun's model is loaded
     */
    @Accessor("modelLoaded")
    boolean tacz_noendman$isModelLoaded();

    /**
     * Reads TaCZ's volatile modelLoadFailed flag.
     *
     * @return true when loading the gun's model failed
     */
    @Accessor("modelLoadFailed")
    boolean tacz_noendman$isModelLoadFailed();
}

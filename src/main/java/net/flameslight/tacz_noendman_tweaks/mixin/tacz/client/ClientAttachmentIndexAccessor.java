package net.flameslight.tacz_noendman_tweaks.mixin.tacz.client;

import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads whether TaCZ finished loading an attachment's models, without waiting for it (TaCZ's getAttachmentModel blocks
 * until the models are loaded). Used by the first equip pre-render.
 */
@Mixin(value = ClientAttachmentIndex.class, remap = false)
public interface ClientAttachmentIndexAccessor {

    /**
     * Reads TaCZ's modelsLoaded flag.
     *
     * @return true when the attachment's models are loaded
     */
    @Accessor("modelsLoaded")
    boolean tacz_noendman$isModelsLoaded();

    /**
     * Reads TaCZ's modelsLoadFailed flag.
     *
     * @return true when loading the attachment's models failed
     */
    @Accessor("modelsLoadFailed")
    boolean tacz_noendman$isModelsLoadFailed();
}

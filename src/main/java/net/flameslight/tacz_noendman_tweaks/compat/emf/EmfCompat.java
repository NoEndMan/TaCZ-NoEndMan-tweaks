package net.flameslight.tacz_noendman_tweaks.compat.emf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

/**
 * Entity Model Features compatibility: whether it is installed, whether it animates a model, and safe access to it.
 * EMF own classes are only used in EmfTwistHook.
 */
public final class EmfCompat {

    /** Mod id of Entity Model Features. */
    public static final String MOD_ID = "entity_model_features";

    /** True when EMF is installed. */
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    /**
     * Registers this mod's EMF animation hook, when EMF is installed.
     */
    public static void registerHook() {
        if (LOADED)
            EmfTwistHook.register();
    }

    /**
     * Tells whether EMF animates the model an entity is drawn with.
     *
     * @param entity the entity
     * @return true when EMF animates its model, false without EMF
     */
    public static boolean isModelAnimated(LivingEntity entity) {
        if (!LOADED)
            return false;

        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        return renderer instanceof LivingEntityRenderer<?, ?> living && EmfTwistHook.isAnimatedByEmf(living.getModel());
    }
}

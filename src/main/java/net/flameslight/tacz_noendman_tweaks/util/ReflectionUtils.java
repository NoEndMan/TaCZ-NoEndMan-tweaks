package net.flameslight.tacz_noendman_tweaks.util;

import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;

import java.lang.reflect.Field;

/**
 * Reflection access to private fields of other mods. Each field is looked up once, on first use, and cached.
 * Classes are named by string, so this class loads even when the other mod is not installed.
 */
public class ReflectionUtils {

    /** Player Animator's animation stack class. */
    public static final String ANIMATION_STACK_CLASS = "dev.kosmx.playerAnim.api.layered.AnimationStack";

    private static Field animationStackLayers;
    private static boolean animationStackLayersResolved = false;

    /**
     * Returns Player Animator's private AnimationStack.layers field, looking it up on the first call.
     *
     * @return the accessible field, or null when the lookup failed
     */
    public static Field getAnimationStackLayersField() {
        if (!animationStackLayersResolved) {
            animationStackLayers = findField(ANIMATION_STACK_CLASS, "layers");
            animationStackLayersResolved = true;
        }
        return animationStackLayers;
    }

    /**
     * Finds a declared field and makes it accessible. Warns when the lookup fails.
     *
     * @param className the fully qualified class name
     * @param fieldName the field name
     * @return the accessible field, or null when the class or field can't be found or opened
     */
    public static Field findField(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLogger.warn("reflection: could not access field {}.{}: {}", className, fieldName, e.toString());
            return null;
        }
    }
}

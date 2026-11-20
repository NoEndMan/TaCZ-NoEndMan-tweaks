package net.flameslight.tacz_noendman_tweaks.logger;

import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The mod's logger: prefixes every line with the mod's name and prints debug lines only when isDebugMod is on.
 */
public class ModLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("tacz_noendman_tweaks");

    private static final Set<String> LOGGED_ONCE = ConcurrentHashMap.newKeySet();

    public static void info(String message, Object ... args) {
        LOGGER.info(buildMessageLog(message), args);
    }

    /**
     * Logs a [DEBUG] line at info level, only when isDebugMod is on.
     */
    public static void debug(String message, Object ... args) {
        if (CommonConfig.isDebugMod())
            LOGGER.info(buildMessageLog("[DEBUG]: " + message), args);
    }

    /**
     * Logs a [DEBUG] line once per game session, only when isDebugMod is on.
     *
     */
    public static void debugOnce(String message) {
        if (CommonConfig.isDebugMod() && LOGGED_ONCE.add(message))
            LOGGER.info(buildMessageLog("[DEBUG]: " + message));
    }

    /**
     * Compares a fast path result with the original code's result and warns when they differ, only when
     * isDebugMod is on. The original code runs only in debug mode.
     *
     */
    public static void debugVerify(String check, Supplier<?> original, Object fastPath) {
        if (CommonConfig.isDebugMod()) {
            Object expected = original.get();
            if (!Objects.equals(expected, fastPath))
                LOGGER.warn(buildMessageLog("[DEBUG]: result differs in {}: original {}, fast path {}"), check, expected, fastPath);
        }
    }

    public static void warn(String message, Object ... args) {
        LOGGER.warn(buildMessageLog(message), args);
    }


    public static void error(String message, Object ... args) {
        LOGGER.error(buildMessageLog(message), args);
    }

    public static void error(String message, Throwable error) {
        LOGGER.error(buildMessageLog(message), error);
    }

    private static String buildMessageLog(String message) {
        return MessageFormat.format("{0}: {1}", "Tacz NoEndMan Tweaks-logger", message);
    }
}

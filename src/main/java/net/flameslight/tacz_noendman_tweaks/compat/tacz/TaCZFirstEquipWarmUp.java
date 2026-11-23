package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

import java.util.ArrayList;

/**
 * Warms up, once per game session and off the render thread, TaCZ's Lua path that runs for the first time on the
 * first gun equip: LuaJ's classes and its reflection tables for TaCZ's script classes. Both are shared by every gun's
 * scripts and kept until the game closes, so the real first equip finds them ready. Client only, only call when
 * TaCZCompat.LOADED is true.
 */
public final class TaCZFirstEquipWarmUp {

    /*
        A Lua function with the kinds of operations TaCZ's gun scripts use, run once. Indexing a method of the TaCZ
        objects builds LuaJ's reflection table for their class, without calling the method.
     */
    private static final String LUA_SCRIPT = """
            return function(api, context, list, name, value)
                local apiMethod = api.getCachedProperty
                local contextMethod = context.getTrackArray
                local data = { name = name, total = 0 }
                for k = 1, 8 do
                    data.total = data.total + k * 0.5
                    data[k] = math.floor(value * k) % 7
                end
                if name == "damage" then
                    value = value * 1.1
                elseif name == "rpm" then
                    value = value + 1
                end
                list:clear()
                list:add(data.total)
                local text = string.format("%s_%d", name, list:size())
                local mask = bit32.band(#text + #data, 255)
                return value + mask * 0 + #table.concat({ "a", "b" }, "") * 0
            end
            """;

    /* Property name passed to the Lua function, as TaCZ's modify_cached_property gets one. */
    private static final LuaValue NAME = LuaValue.valueOf("damage");

    /*
        Done flag of this game session, read by the warm-up thread and the client thread.
     */
    private static volatile boolean done = false;

    /*
        Client thread only: the warm-up thread.
     */
    private static Thread thread = null;

    /**
     * Registers the world join listener. Called once from client setup.
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TaCZFirstEquipWarmUp::onLoggingIn);
    }

    /**
     * World join: starts the warm-up thread, when the warm-up is still not done this session.
     *
     * @param event the login event
     */
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (done || !ClientConfig.isFirstEquipWarmUp() || (thread != null && thread.isAlive()))
            return;

        thread = new Thread(TaCZFirstEquipWarmUp::run, "tacz_noendman first equip warm-up");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * The warm-up thread: runs the Lua warm-up once.
     */
    private static void run() {
        long start = System.nanoTime();

        try {
            warmUpLua();
            ModLogger.debug("lua warm-up: {} ms", (System.nanoTime() - start) / 1_000_000L);
        } catch (Throwable error) {
            ModLogger.warn("First equip warm-up: Lua warm-up failed, skipped for this session: {}", error.toString());
        }

        // Also marked done when it failed, so a failing warm-up is not retried on every world join.
        done = true;
    }

    /**
     * Runs a Lua function once in a private LuaJ Globals set up like TaCZ's script globals, with fresh TaCZ script
     * objects, so LuaJ's classes load and its reflection tables for TaCZ's script classes are built. Touches no game
     * state and none of TaCZ's own scripts or globals.
     */
    private static void warmUpLua() {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new JseStringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);

        LuaValue function = globals.load(LUA_SCRIPT, "tacz_noendman_warm_up").call();

        /* The same coercions TaCZ uses: its data script gets a ModernKineticGunScriptAPI, its animation script a
           GunAnimationStateContext. Both constructors only set plain fields. */
        LuaValue api = CoerceJavaToLua.coerce(new ModernKineticGunScriptAPI());
        LuaValue context = CoerceJavaToLua.coerce(new GunAnimationStateContext());
        LuaValue list = CoerceJavaToLua.coerce(new ArrayList<Double>());
        LuaValue value = CoerceJavaToLua.coerce(1.0F);
        LuaValue result = function.invoke(LuaValue.varargsOf(new LuaValue[]{api, context, list, NAME, value})).arg1();
        CoerceLuaToJava.coerce(result, Float.class);
    }
}

package net.flameslight.tacz_noendman_tweaks.events;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.client.ClientSetup;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.handlers.SetupHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Mod bus listeners of both sides. Each one only forwards to its handler.
 */
@Mod.EventBusSubscriber(modid = TaczNoEndManTweaks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    /**
     * Config load, reload and unload.
     *
     * @param event any mod config event
     */
    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent event) {
        CommonConfig.onConfigLoaded(event.getConfig(), event instanceof ModConfigEvent.Unloading);
    }

    /**
     * Common setup.
     *
     * @param event the common setup event
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        SetupHandler.logIntegrations();
    }

    /**
     * Client setup. Only fired on the client.
     *
     * @param event the client setup event
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }
}

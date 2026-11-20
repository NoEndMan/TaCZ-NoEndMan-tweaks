package net.flameslight.tacz_noendman_tweaks.events;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.config.CommonConfig;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
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
        boolean unloading = event instanceof ModConfigEvent.Unloading;
        CommonConfig.onConfigLoaded(event.getConfig(), unloading);
        ClientConfig.onConfigLoaded(event.getConfig(), unloading);
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        ModList mods = ModList.get();
        ModLogger.debug("Integrations: Entity Model Features {}, Player Animator {}",
                mods.isLoaded(EmfCompat.MOD_ID), mods.isLoaded(PlayerAnimatorCompat.MOD_ID));
    }

    /**
     * Client setup. Only fired on the client.
     *
     * @param event the client setup event
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EmfCompat::registerHook);
        event.enqueueWork(PlayerAnimatorCompat::registerLayers);
    }
}

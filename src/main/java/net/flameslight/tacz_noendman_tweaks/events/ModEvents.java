package net.flameslight.tacz_noendman_tweaks.events;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.compat.emf.EmfCompat;
import net.flameslight.tacz_noendman_tweaks.compat.playeranimator.PlayerAnimatorCompat;
import net.flameslight.tacz_noendman_tweaks.compat.tacz.TaczCompat;
import net.flameslight.tacz_noendman_tweaks.compat.tacz.TaczEarlyStencil;
import net.flameslight.tacz_noendman_tweaks.compat.tacz.TaczFirstEquipPreRender;
import net.flameslight.tacz_noendman_tweaks.compat.tacz.TaczFirstEquipWarmUp;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Mod bus listeners of both sides. Each one only forwards to its handler.
 */
@Mod.EventBusSubscriber(modid = TaczNoEndManTweaks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        ModList mods = ModList.get();
        ModLogger.debug("Integrations: TACZ {}, Entity Model Features {}, Player Animator {}",
                mods.isLoaded(TaczCompat.MOD_ID), mods.isLoaded(EmfCompat.MOD_ID), mods.isLoaded(PlayerAnimatorCompat.MOD_ID));
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

        if (TaczCompat.LOADED) {
            TaczFirstEquipWarmUp.register();
            TaczFirstEquipPreRender.register();
            event.enqueueWork(TaczEarlyStencil::enable);
        }
    }
}

package net.flameslight.tacz_noendman_tweaks.events;

import net.flameslight.tacz_noendman_tweaks.TaczNoEndManTweaks;
import net.flameslight.tacz_noendman_tweaks.client.TwistFade;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge bus listeners of the client. Each one only forwards to a handler.
 */
@Mod.EventBusSubscriber(modid = TaczNoEndManTweaks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    /**
     * End of a client tick: updates the upper body twist strength of every player.
     *
     * @param event the client tick event
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TwistFade.onClientTick();
        }
    }
}

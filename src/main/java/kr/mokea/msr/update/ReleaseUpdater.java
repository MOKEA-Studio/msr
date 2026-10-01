package kr.mokea.msr.update;

import kr.mokea.msr.MsrMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReleaseUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseUpdater.class);

    private ReleaseUpdater() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        UpdateService.startCheck(message -> LOGGER.info("[MSR] {}", message));
    }

    public static void manualCheck() {
        UpdateService.startCheck(ReleaseUpdater::tellPlayer);
    }

    private static void tellPlayer(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) minecraft.player.displayClientMessage(Component.literal("[MSR] " + message), false);
            else LOGGER.info("[MSR] {}", message);
        });
    }
}

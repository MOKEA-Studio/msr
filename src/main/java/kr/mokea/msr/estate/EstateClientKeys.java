package kr.mokea.msr.estate;

import com.mojang.blaze3d.platform.InputConstants;
import kr.mokea.msr.MsrMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class EstateClientKeys {
    private static final KeyMapping OPEN = new KeyMapping("key.msr.estates", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, KeyMapping.CATEGORY_INTERFACE);

    private EstateClientKeys() {}

    @EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }
    }

    @EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static final class GameEvents {
        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) minecraft.setScreen(new EstateScreen());
            }
        }
    }
}

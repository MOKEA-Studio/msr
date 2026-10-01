package kr.mokea.msr.update;

import kr.mokea.msr.MsrMod;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.DEDICATED_SERVER, bus = EventBusSubscriber.Bus.GAME)
public final class ServerUpdateHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerUpdateHooks.class);

    private ServerUpdateHooks() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        UpdateService.startCheck(message -> LOGGER.info("[MSR] {}", message));
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("msr")
                .then(literal("update")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            var source = context.getSource();
                            UpdateService.startCheck(message -> source.getServer().execute(() ->
                                    source.sendSuccess(() -> Component.literal("[MSR] " + message), true)));
                            return 1;
                        })));
    }
}

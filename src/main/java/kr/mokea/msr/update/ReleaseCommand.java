package kr.mokea.msr.update;

import kr.mokea.msr.MsrMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ReleaseCommand {
    private ReleaseCommand() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("msr")
                .then(literal("update").executes(context -> {
                    ReleaseUpdater.startCheck(true);
                    return 1;
                })));
    }
}

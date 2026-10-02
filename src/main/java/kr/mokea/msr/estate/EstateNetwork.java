package kr.mokea.msr.estate;

import kr.mokea.msr.MsrMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = MsrMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class EstateNetwork {
    private EstateNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(EstateActionPayload.TYPE, EstateActionPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                EstateService.handle(player, payload.action());
            }
        });
        registrar.playToServer(EstateClaimActionPayload.TYPE, EstateClaimActionPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                EstateService.handleClaimAction(player, payload);
            }
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(EstateStatePayload.TYPE, EstateStatePayload.CODEC, ClientEstateState::receive);
            registrar.playToClient(EstateClaimsPayload.TYPE, EstateClaimsPayload.CODEC, (payload, context) -> {
                ClientClaimMap.receive(payload, context);
                context.enqueueWork(XaeroBridge::sync);
            });
        } else {
            registrar.playToClient(EstateStatePayload.TYPE, EstateStatePayload.CODEC, (payload, context) -> {});
            registrar.playToClient(EstateClaimsPayload.TYPE, EstateClaimsPayload.CODEC, (payload, context) -> {});
        }
    }
}

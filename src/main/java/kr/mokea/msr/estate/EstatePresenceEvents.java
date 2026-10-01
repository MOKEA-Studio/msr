package kr.mokea.msr.estate;

import kr.mokea.msr.MsrMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = MsrMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class EstatePresenceEvents {
    private static final Map<UUID, String> LAST_CHUNK = new HashMap<>();
    private static final Map<UUID, String> LAST_DIMENSION = new HashMap<>();

    private EstatePresenceEvents() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        String dimension = player.serverLevel().dimension().location().toString();
        if (!dimension.equals(LAST_DIMENSION.put(id, dimension))) EstateService.sendClaims(player);

        String chunk = EstateService.key(player.serverLevel(), player.blockPosition());
        if (chunk.equals(LAST_CHUNK.put(id, chunk))) return;
        EstateData.Claim claim = EstateData.get(player.getServer()).claim(chunk);
        if (claim != null) {
            player.displayClientMessage(Component.literal("[" + claim.ownerName() + "]의 청크에 들어와있습니다."), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_CHUNK.remove(player.getUUID());
            LAST_DIMENSION.remove(player.getUUID());
        }
    }
}

package kr.mokea.msr.estate;

import kr.mokea.msr.MsrMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Objects;

@EventBusSubscriber(modid = MsrMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class EstateEvents {
    private EstateEvents() {}

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level
                && !EstateService.mayEdit(player, level, event.getPos())) {
            event.setCanceled(true);
            EstateService.deny(player);
        }
    }

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        boolean blocked = !EstateService.mayEdit(player, level, event.getPos());
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            blocked |= multi.getReplacedBlockSnapshots().stream()
                    .anyMatch(snapshot -> !EstateService.mayEdit(player, level, snapshot.getPos()));
        }
        if (blocked) {
            event.setCanceled(true);
            EstateService.deny(player);
        }
    }

    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level
                && !EstateService.mayEdit(player, level, event.getPos())) {
            event.setCanceled(true);
            EstateService.deny(player);
        }
    }

    @SubscribeEvent
    public static void fluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        EstateData data = EstateData.get(level.getServer());
        EstateData.Claim source = data.claim(EstateService.key(level, event.getLiquidPos()));
        EstateData.Claim target = data.claim(EstateService.key(level, event.getPos()));
        if (!sameOwner(source, target)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var resolver = event.getStructureHelper();
        if (!resolver.resolve()) return;
        EstateData data = EstateData.get(level.getServer());
        EstateData.Claim pistonClaim = data.claim(EstateService.key(level, event.getPos()));
        for (var pos : resolver.getToPush()) {
            if (!sameOwner(pistonClaim, data.claim(EstateService.key(level, pos)))
                    || !sameOwner(pistonClaim, data.claim(EstateService.key(level, pos.relative(resolver.getPushDirection()))))) {
                event.setCanceled(true);
                return;
            }
        }
        for (var pos : resolver.getToDestroy()) {
            if (!sameOwner(pistonClaim, data.claim(EstateService.key(level, pos)))) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean sameOwner(EstateData.Claim a, EstateData.Claim b) {
        return Objects.equals(a == null ? null : a.owner(), b == null ? null : b.owner());
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        EstateData data = EstateData.get(level.getServer());
        event.getAffectedBlocks().removeIf(pos -> data.claim(EstateService.key(level, pos)) != null);
    }
}

package kr.mokea.msr.estate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class EstateService {
    private EstateService() {}

    public static String key(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return level.dimension().location() + ":" + chunk.x + ":" + chunk.z;
    }

    public static boolean mayEdit(ServerPlayer player, ServerLevel level, BlockPos pos) {
        EstateData.Claim claim = EstateData.get(player.getServer()).claim(key(level, pos));
        return claim == null || claim.owner().equals(player.getUUID()) || player.hasPermissions(2);
    }

    public static void deny(ServerPlayer player) {
        player.displayClientMessage(Component.literal("§c이 청크는 다른 플레이어의 소유입니다."), true);
    }

    public static void handle(ServerPlayer player, String action) {
        EstateData data = EstateData.get(player.getServer());
        String key = key(player.serverLevel(), player.blockPosition());
        String message = "";
        switch (action) {
            case "buy" -> {
                if (data.claim(key) != null) message = "이미 소유자가 있는 청크입니다.";
                else if (data.balance(player.getUUID()) < EstateData.CHUNK_PRICE) message = "잔액이 부족합니다.";
                else if (data.buy(player.getUUID(), player.getGameProfile().getName(), key)) message = "청크를 구매했습니다.";
            }
            case "release" -> message = data.release(player.getUUID(), key)
                    ? "청크 소유권을 해제했습니다. 환불은 없습니다." : "내 청크가 아닙니다.";
            case "sell_iron" -> message = sell(player, data, Items.IRON_INGOT, 1_000L, "철 주괴");
            case "sell_gold" -> message = sell(player, data, Items.GOLD_INGOT, 5_000L, "금 주괴");
            case "sell_netherite" -> message = sell(player, data, Items.NETHERITE_INGOT, 100_000L, "네더라이트 주괴");
            case "status" -> {}
            default -> message = "알 수 없는 작업입니다.";
        }
        sendState(player, message);
    }

    private static String sell(ServerPlayer player, EstateData data, Item item, long unitPrice, String label) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        if (count == 0) return label + "가 인벤토리에 없습니다.";
        long proceeds = Math.multiplyExact(count, unitPrice);
        // Check overflow before consuming items.
        try {
            Math.addExact(data.balance(player.getUUID()), proceeds);
        } catch (ArithmeticException overflow) {
            return "잔액 한도에 도달했습니다.";
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) stack.shrink(stack.getCount());
        }
        player.getInventory().setChanged();
        data.credit(player.getUUID(), proceeds);
        return label + " " + count + "개를 판매했습니다.";
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    public static void sendState(ServerPlayer player, String message) {
        EstateData data = EstateData.get(player.getServer());
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        EstateData.Claim claim = data.claim(key(player.serverLevel(), player.blockPosition()));
        UUID owner = claim == null ? null : claim.owner();
        PacketDistributor.sendToPlayer(player, new EstateStatePayload(
                data.balance(player.getUUID()), chunk.x, chunk.z,
                player.serverLevel().dimension().location().toString(),
                owner == null ? "" : claim.ownerName(), owner != null && owner.equals(player.getUUID()),
                data.claimCount(player.getUUID()),
                count(player, Items.IRON_INGOT), count(player, Items.GOLD_INGOT), count(player, Items.NETHERITE_INGOT), message));
    }
}

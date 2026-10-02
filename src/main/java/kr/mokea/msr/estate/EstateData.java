package kr.mokea.msr.estate;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EstateData extends SavedData {
    public static final long CHUNK_PRICE = 100_000L;
    private final Map<UUID, Long> balances = new HashMap<>();
    private final Map<String, Claim> claims = new HashMap<>();

    public record Claim(UUID owner, String ownerName, int color) {}

    public static EstateData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(EstateData::new, EstateData::load), "msr_estate");
    }

    static EstateData load(CompoundTag tag, HolderLookup.Provider registries) {
        EstateData data = new EstateData();
        ListTag savedBalances = tag.getList("Balances", Tag.TAG_COMPOUND);
        for (int i = 0; i < savedBalances.size(); i++) {
            CompoundTag entry = savedBalances.getCompound(i);
            try {
                data.balances.put(entry.getUUID("Player"), Math.max(0, entry.getLong("Amount")));
            } catch (IllegalArgumentException ignored) {}
        }
        ListTag savedClaims = tag.getList("Claims", Tag.TAG_COMPOUND);
        for (int i = 0; i < savedClaims.size(); i++) {
            CompoundTag entry = savedClaims.getCompound(i);
            try {
                data.claims.put(entry.getString("Key"),
                        new Claim(entry.getUUID("Owner"), entry.getString("Name"), entry.getInt("Color")));
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag savedBalances = new ListTag();
        balances.forEach((player, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putLong("Amount", amount);
            savedBalances.add(entry);
        });
        tag.put("Balances", savedBalances);
        ListTag savedClaims = new ListTag();
        claims.forEach((key, claim) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Key", key);
            entry.putUUID("Owner", claim.owner());
            entry.putString("Name", claim.ownerName());
            entry.putInt("Color", claim.color());
            savedClaims.add(entry);
        });
        tag.put("Claims", savedClaims);
        return tag;
    }

    public long balance(UUID player) {
        return balances.getOrDefault(player, 0L);
    }

    public int claimCount(UUID player) {
        return (int) claims.values().stream().filter(c -> c.owner().equals(player)).count();
    }

    public List<EstateClaimsPayload.Chunk> ownedChunks(UUID player, String dimension) {
        String prefix = dimension + ":";
        List<EstateClaimsPayload.Chunk> result = new ArrayList<>();
        for (var entry : claims.entrySet()) {
            if (!entry.getValue().owner().equals(player) || !entry.getKey().startsWith(prefix)) continue;
            if (result.size() >= EstateClaimsPayload.MAX_CHUNKS) break;
            String coordinates = entry.getKey().substring(prefix.length());
            int separator = coordinates.indexOf(':');
            if (separator < 0) continue;
            try {
                result.add(new EstateClaimsPayload.Chunk(
                        Integer.parseInt(coordinates.substring(0, separator)),
                        Integer.parseInt(coordinates.substring(separator + 1)),
                        entry.getValue().color()));
            } catch (NumberFormatException ignored) {}
        }
        return List.copyOf(result);
    }

    public Claim claim(String key) {
        return claims.get(key);
    }

    public void credit(UUID player, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative credit");
        balances.put(player, Math.addExact(balance(player), amount));
        setDirty();
    }

    public boolean buy(UUID player, String name, String key, int color) {
        if (claims.containsKey(key) || balance(player) < CHUNK_PRICE) return false;
        balances.put(player, balance(player) - CHUNK_PRICE);
        claims.put(key, new Claim(player, name, color));
        setDirty();
        return true;
    }

    /** Fills in a terrain color sampled after the fact (e.g. for claims saved before colors existed). */
    public void updateColor(String key, int color) {
        Claim claim = claims.get(key);
        if (claim == null || color == 0) return;
        claims.put(key, new Claim(claim.owner(), claim.ownerName(), color));
        setDirty();
    }

    public boolean release(UUID player, String key) {
        Claim claim = claims.get(key);
        if (claim == null || !claim.owner().equals(player)) return false;
        claims.remove(key);
        setDirty();
        return true;
    }
}

package kr.mokea.msr.estate;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ClientClaimMap {
    private static String dimension = "";
    private static List<EstateClaimsPayload.Chunk> chunks = List.of();
    private static Map<Long, EstateClaimsPayload.Chunk> byPosition = Map.of();

    private ClientClaimMap() {}

    public static void receive(EstateClaimsPayload payload, IPayloadContext context) {
        dimension = payload.dimension();
        chunks = List.copyOf(payload.chunks());
        byPosition = chunks.stream().collect(Collectors.toUnmodifiableMap(c -> key(c.x(), c.z()), c -> c));
    }

    public static void clear() {
        dimension = "";
        chunks = List.of();
        byPosition = Map.of();
    }

    public static String dimension() { return dimension; }
    public static List<EstateClaimsPayload.Chunk> chunks() { return chunks; }
    public static boolean owns(String dimensionId, int x, int z) {
        return dimension.equals(dimensionId) && byPosition.containsKey(key(x, z));
    }
    /** Packed ARGB terrain swatch for an owned chunk, or 0 if unowned or not yet sampled. */
    public static int colorOf(String dimensionId, int x, int z) {
        if (!dimension.equals(dimensionId)) return 0;
        EstateClaimsPayload.Chunk chunk = byPosition.get(key(x, z));
        return chunk == null ? 0 : chunk.color();
    }
    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}

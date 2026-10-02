package kr.mokea.msr.estate;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ClientClaimMap {
    private static String dimension = "";
    private static List<EstateClaimsPayload.Chunk> chunks = List.of();
    private static Map<Long, Integer> colors = Map.of();

    private ClientClaimMap() {}

    public static void receive(EstateClaimsPayload payload, IPayloadContext context) {
        dimension = payload.dimension();
        chunks = List.copyOf(payload.chunks());
        colors = chunks.stream().collect(Collectors.toUnmodifiableMap(c -> key(c.x(), c.z()), EstateClaimsPayload.Chunk::color));
    }

    public static void clear() {
        dimension = "";
        chunks = List.of();
        colors = Map.of();
    }

    public static String dimension() { return dimension; }
    public static List<EstateClaimsPayload.Chunk> chunks() { return chunks; }
    public static boolean owns(String dimensionId, int x, int z) {
        return dimension.equals(dimensionId) && colors.containsKey(key(x, z));
    }
    /** Packed ARGB terrain swatch for an owned chunk, or 0 if unowned or not yet sampled. */
    public static int colorOf(String dimensionId, int x, int z) {
        if (!dimension.equals(dimensionId)) return 0;
        return colors.getOrDefault(key(x, z), 0);
    }
    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}

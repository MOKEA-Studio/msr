package kr.mokea.msr.estate;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClientClaimMap {
    private static String dimension = "";
    private static List<EstateClaimsPayload.Chunk> chunks = List.of();
    private static Set<Long> positions = Set.of();

    private ClientClaimMap() {}

    public static void receive(EstateClaimsPayload payload, IPayloadContext context) {
        dimension = payload.dimension();
        chunks = List.copyOf(payload.chunks());
        positions = chunks.stream().map(c -> key(c.x(), c.z())).collect(Collectors.toUnmodifiableSet());
    }

    public static void clear() {
        dimension = "";
        chunks = List.of();
        positions = Set.of();
    }

    public static String dimension() { return dimension; }
    public static List<EstateClaimsPayload.Chunk> chunks() { return chunks; }
    public static boolean owns(String dimensionId, int x, int z) {
        return dimension.equals(dimensionId) && positions.contains(key(x, z));
    }
    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}

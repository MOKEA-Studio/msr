package kr.mokea.msr.estate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record EstateClaimsPayload(String dimension, List<Chunk> chunks) implements CustomPacketPayload {
    public static final int MAX_CHUNKS = 8192;
    /** color is a packed ARGB terrain swatch computed server-side, or 0 while not yet sampled. */
    public record Chunk(int x, int z, int color, String name) {}

    public static final Type<EstateClaimsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("msr", "estate_claims"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EstateClaimsPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.dimension, 256);
                buf.writeVarInt(payload.chunks.size());
                for (Chunk chunk : payload.chunks) {
                    buf.writeInt(chunk.x());
                    buf.writeInt(chunk.z());
                    buf.writeInt(chunk.color());
                    buf.writeUtf(chunk.name(), 32);
                }
            },
            buf -> {
                String dimension = buf.readUtf(256);
                int size = buf.readVarInt();
                if (size < 0 || size > MAX_CHUNKS) throw new IllegalArgumentException("Invalid claim count: " + size);
                List<Chunk> chunks = new ArrayList<>(size);
                for (int i = 0; i < size; i++) chunks.add(new Chunk(buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(32)));
                return new EstateClaimsPayload(dimension, List.copyOf(chunks));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

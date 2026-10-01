package kr.mokea.msr.estate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EstateStatePayload(long balance, int chunkX, int chunkZ, String dimension,
                                 String ownerName, boolean ownedByYou, int claimCount, int ironCount, int goldCount, int netheriteCount, String message)
        implements CustomPacketPayload {
    public static final Type<EstateStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("msr", "estate_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EstateStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.balance);
                buf.writeInt(payload.chunkX);
                buf.writeInt(payload.chunkZ);
                buf.writeUtf(payload.dimension, 128);
                buf.writeUtf(payload.ownerName, 64);
                buf.writeBoolean(payload.ownedByYou);
                buf.writeInt(payload.claimCount);
                buf.writeInt(payload.ironCount);
                buf.writeInt(payload.goldCount);
                buf.writeInt(payload.netheriteCount);
                buf.writeUtf(payload.message, 256);
            },
            buf -> new EstateStatePayload(buf.readLong(), buf.readInt(), buf.readInt(), buf.readUtf(128),
                    buf.readUtf(64), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(256)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

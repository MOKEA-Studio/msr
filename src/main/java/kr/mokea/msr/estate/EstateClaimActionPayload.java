package kr.mokea.msr.estate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server actions that target a specific (possibly remote) claimed chunk, e.g. from the land map or manage screen. */
public record EstateClaimActionPayload(String action, String dimension, int x, int z, String text) implements CustomPacketPayload {
    public static final Type<EstateClaimActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("msr", "estate_claim_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EstateClaimActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.action, 32);
                buf.writeUtf(payload.dimension, 128);
                buf.writeInt(payload.x);
                buf.writeInt(payload.z);
                buf.writeUtf(payload.text, 32);
            },
            buf -> new EstateClaimActionPayload(buf.readUtf(32), buf.readUtf(128), buf.readInt(), buf.readInt(), buf.readUtf(32)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package kr.mokea.msr.estate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EstateActionPayload(String action) implements CustomPacketPayload {
    public static final Type<EstateActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("msr", "estate_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EstateActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.action, 32),
            buf -> new EstateActionPayload(buf.readUtf(32)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package kr.mokea.msr.update;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class UpdateMessages {
    private UpdateMessages() {}

    public static MutableComponent styled(String message) {
        return Component.literal("[MSR] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }
}

package kr.mokea.msr.estate;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientEstateState {
    private ClientEstateState() {}

    public static void receive(EstateStatePayload state, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof EstateScreen screen) screen.accept(state);
    }
}

package kr.mokea.msr.estate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** A small text-entry prompt for naming a claimed chunk, parented back to the manage screen. */
public final class EstateRenameScreen extends Screen {
    private final EstateManageScreen previous;
    private final EstateClaimsPayload.Chunk chunk;
    private EditBox input;

    public EstateRenameScreen(EstateManageScreen previous, EstateClaimsPayload.Chunk chunk) {
        super(Component.literal("MSR Rename Claim"));
        this.previous = previous;
        this.chunk = chunk;
    }

    @Override
    protected void init() {
        int w = 220;
        input = new EditBox(font, (width - w) / 2, height / 2 - 4, w, 20, Component.literal("청크 이름"));
        input.setMaxLength(24);
        input.setValue(chunk.name());
        addRenderableWidget(input);
        setInitialFocus(input);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x90070B10);
        g.drawCenteredString(font, "[" + chunk.x() + ", " + chunk.z() + "] 청크 이름 지정",
                width / 2, height / 2 - 24, 0xFFF5F7FB);
        g.drawCenteredString(font, "Enter: 확인  ·  Esc: 취소", width / 2, height / 2 + 24, 0xFF9CAFC5);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        PacketDistributor.sendToServer(new EstateClaimActionPayload(
                "rename", ClientClaimMap.dimension(), chunk.x(), chunk.z(), input.getValue()));
        previous.notice("서버에서 처리하는 중...");
        minecraft.setScreen(previous);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

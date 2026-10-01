package kr.mokea.msr.estate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/** A real chunk map: cyan cells are owned land, the gold cell is the player. */
public final class EstateMapScreen extends Screen {
    private final Screen previous;
    private int centerX;
    private int centerZ;
    private int selected = -1;
    private List<EstateClaimsPayload.Chunk> claims = List.of();

    public EstateMapScreen(Screen previous) {
        super(Component.literal("MSR Land Map"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) return;
        centerX = minecraft.player.chunkPosition().x;
        centerZ = minecraft.player.chunkPosition().z;
        int px = centerX;
        int pz = centerZ;
        refreshClaims(px, pz);
    }

    @Override
    public void tick() {
        if (minecraft != null && minecraft.player != null && claims.size() != ClientClaimMap.chunks().size()) {
            refreshClaims(minecraft.player.chunkPosition().x, minecraft.player.chunkPosition().z);
        }
    }

    private void refreshClaims(int px, int pz) {
        String dimension = minecraft == null || minecraft.level == null ? "" : minecraft.level.dimension().location().toString();
        claims = (dimension.equals(ClientClaimMap.dimension()) ? ClientClaimMap.chunks() : List.<EstateClaimsPayload.Chunk>of()).stream()
                .sorted(Comparator.comparingLong(c -> Math.abs((long)c.x() - px) + Math.abs((long)c.z() - pz)))
                .toList();
        selected = -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x4207121D);
        int w = Math.min(390, width - 18);
        int h = Math.min(226, height - 10);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xFF47D6CE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xF017202D);
        g.fillGradient(x + 2, y + 2, x + w - 2, y + 33, 0xFF25384B, 0xFF1D2A3B);
        g.drawString(font, "MSR  /  LAND MAP", x + 17, y + 14, 0xFFF5F7FB);
        int mapX = x + 12;
        int mapY = y + 40;
        g.fill(mapX - 1, mapY - 1, mapX + 171, mapY + 171, 0xFF3F526B);
        g.fill(mapX, mapY, mapX + 170, mapY + 170, 0xFF172435);
        String dimension = minecraft == null || minecraft.level == null ? "" : minecraft.level.dimension().location().toString();
        int playerX = minecraft == null || minecraft.player == null ? centerX : minecraft.player.chunkPosition().x;
        int playerZ = minecraft == null || minecraft.player == null ? centerZ : minecraft.player.chunkPosition().z;
        EstateClientVisuals.drawGrid(g, mapX, mapY, 17, 10, dimension, centerX, centerZ, playerX, playerZ);

        int sideX = x + 195;
        int sideW = w - 207;
        g.fill(sideX, y + 40, sideX + sideW, y + 94, 0xFF253247);
        g.drawString(font, "내 소유 청크", sideX + 9, y + 49, 0xFF9CAFC5);
        g.drawString(font, Integer.toString(claims.size()), sideX + 9, y + 67, 0xFF47D6CE);
        g.fill(sideX, y + 101, sideX + sideW, y + 147, 0xFF253247);
        g.drawString(font, "지도 중심", sideX + 9, y + 109, 0xFF9CAFC5);
        g.drawString(font, centerX + ", " + centerZ, sideX + 9, y + 126, 0xFFF5F7FB);
        button(g, sideX, y + 155, (sideW - 6) / 2, 22, "이전 땅", mouseX, mouseY);
        button(g, sideX + (sideW + 6) / 2, y + 155, (sideW - 6) / 2, 22, "다음 땅", mouseX, mouseY);
        button(g, sideX, y + 182, (sideW - 6) / 2, 22, "내 위치", mouseX, mouseY);
        button(g, sideX + (sideW + 6) / 2, y + 182, (sideW - 6) / 2, 22, "돌아가기", mouseX, mouseY);
        g.drawString(font, "청록: 내 땅  ·  금색: 내 위치  ·  방향키 이동", x + 12, y + h - 12, 0xFF9CAFC5);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? 0xFF47D6CE : 0xFF496782);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFF385367 : 0xFF294052);
        g.drawCenteredString(font, label, x + w / 2, y + 7, 0xFFF5F7FB);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int w = Math.min(390, width - 18);
        int h = Math.min(226, height - 10);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int mapX = x + 12;
        int mapY = y + 40;
        if (inside(mx, my, mapX, mapY, 170, 170)) {
            centerX += (int)(mx - mapX) / 10 - 8;
            centerZ += (int)(my - mapY) / 10 - 8;
            return true;
        }
        int sideX = x + 195;
        int sideW = w - 207;
        int half = (sideW - 6) / 2;
        if (inside(mx, my, sideX, y + 155, half, 22)) focus(-1);
        else if (inside(mx, my, sideX + half + 6, y + 155, half, 22)) focus(1);
        else if (inside(mx, my, sideX, y + 182, half, 22)) {
            if (minecraft.player != null) {
                centerX = minecraft.player.chunkPosition().x;
                centerZ = minecraft.player.chunkPosition().z;
            }
        } else if (inside(mx, my, sideX + half + 6, y + 182, half, 22)) onClose();
        else return super.mouseClicked(mx, my, button);
        return true;
    }

    private void focus(int step) {
        if (claims.isEmpty()) return;
        selected = selected < 0 ? (step > 0 ? 0 : claims.size() - 1) : Math.floorMod(selected + step, claims.size());
        var claim = claims.get(selected);
        centerX = claim.x();
        centerZ = claim.z();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) centerX--;
        else if (keyCode == GLFW.GLFW_KEY_RIGHT) centerX++;
        else if (keyCode == GLFW.GLFW_KEY_UP) centerZ--;
        else if (keyCode == GLFW.GLFW_KEY_DOWN) centerZ++;
        else return super.keyPressed(keyCode, scanCode, modifiers);
        return true;
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

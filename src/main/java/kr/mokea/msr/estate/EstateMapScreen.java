package kr.mokea.msr.estate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/** A real chunk map: owned land is marked with cyan diamonds on sampled terrain colors, gold marks the player. */
public final class EstateMapScreen extends Screen {
    private static final int CELL = 9;
    private static final int CELLS = 17;
    private static final int GRID = CELL * CELLS;

    private final Screen previous;
    private int centerX;
    private int centerZ;
    private int selected = -1;
    private List<EstateClaimsPayload.Chunk> claims = List.of();
    private List<EstateClaimsPayload.Chunk> lastSource = List.of();
    private String notice = "";

    public EstateMapScreen(Screen previous) {
        super(Component.literal("MSR Land Map"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) return;
        centerX = minecraft.player.chunkPosition().x;
        centerZ = minecraft.player.chunkPosition().z;
        refreshClaims(centerX, centerZ);
    }

    @Override
    public void tick() {
        if (minecraft != null && minecraft.player != null && ClientClaimMap.chunks() != lastSource) {
            refreshClaims(minecraft.player.chunkPosition().x, minecraft.player.chunkPosition().z);
        }
    }

    private void refreshClaims(int px, int pz) {
        EstateClaimsPayload.Chunk previouslySelected = selected >= 0 && selected < claims.size() ? claims.get(selected) : null;
        String dimension = minecraft == null || minecraft.level == null ? "" : minecraft.level.dimension().location().toString();
        lastSource = ClientClaimMap.chunks();
        claims = (dimension.equals(ClientClaimMap.dimension()) ? lastSource : List.<EstateClaimsPayload.Chunk>of()).stream()
                .sorted(Comparator.comparingLong(c -> Math.abs((long)c.x() - px) + Math.abs((long)c.z() - pz)))
                .toList();
        selected = -1;
        if (previouslySelected != null) {
            for (int i = 0; i < claims.size(); i++) {
                if (claims.get(i).x() == previouslySelected.x() && claims.get(i).z() == previouslySelected.z()) {
                    selected = i;
                    break;
                }
            }
        }
    }

    private record Layout(int x, int y, int w, int h, int mapX, int mapY, int sideX, int sideW) {}

    private Layout layout() {
        int w = Math.min(430, width - 18);
        int h = Math.min(248, height - 10);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int mapX = x + 30;
        int mapY = y + 54;
        int sideX = mapX + GRID + 24;
        int sideW = x + w - sideX - 12;
        return new Layout(x, y, w, h, mapX, mapY, sideX, sideW);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0x4207121D); // Keep the world sharp behind the dashboard.
        g.fill(l.x, l.y, l.x + l.w, l.y + l.h, 0xFF47D6CE);
        g.fill(l.x + 1, l.y + 1, l.x + l.w - 1, l.y + l.h - 1, 0xF017202D);
        g.fillGradient(l.x + 2, l.y + 2, l.x + l.w - 2, l.y + 33, 0xFF25384B, 0xFF1D2A3B);
        g.drawString(font, "MSR  /  LAND MAP", l.x + 17, l.y + 14, 0xFFF5F7FB);
        String dimension = minecraft == null || minecraft.level == null ? "" : minecraft.level.dimension().location().toString();
        int playerX = minecraft == null || minecraft.player == null ? centerX : minecraft.player.chunkPosition().x;
        int playerZ = minecraft == null || minecraft.player == null ? centerZ : minecraft.player.chunkPosition().z;
        EstateClientVisuals.drawGrid(g, l.mapX, l.mapY, CELLS, CELL, dimension, centerX, centerZ, playerX, playerZ, true);

        g.fill(l.sideX, l.y + 40, l.sideX + l.sideW, l.y + 94, 0xFF253247);
        g.drawString(font, "내 소유 청크", l.sideX + 9, l.y + 49, 0xFF9CAFC5);
        g.drawString(font, Integer.toString(claims.size()), l.sideX + 9, l.y + 67, 0xFF47D6CE);
        g.fill(l.sideX, l.y + 101, l.sideX + l.sideW, l.y + 147, 0xFF253247);
        if (selected >= 0 && selected < claims.size()) {
            var claim = claims.get(selected);
            String label = claim.name().isEmpty() ? "이름 없는 땅" : claim.name();
            g.drawString(font, trim(label, l.sideW - 18), l.sideX + 9, l.y + 109, 0xFF47D6CE);
        } else {
            g.drawString(font, "지도 중심", l.sideX + 9, l.y + 109, 0xFF9CAFC5);
        }
        g.drawString(font, centerX + ", " + centerZ, l.sideX + 9, l.y + 126, 0xFFF5F7FB);
        button(g, l.sideX, l.y + 155, (l.sideW - 6) / 2, 22, "이전 땅", mouseX, mouseY);
        button(g, l.sideX + (l.sideW + 6) / 2, l.y + 155, (l.sideW - 6) / 2, 22, "다음 땅", mouseX, mouseY);
        button(g, l.sideX, l.y + 182, (l.sideW - 6) / 2, 22, "내 위치", mouseX, mouseY);
        button(g, l.sideX + (l.sideW + 6) / 2, l.y + 182, (l.sideW - 6) / 2, 22, "돌아가기", mouseX, mouseY);
        button(g, l.sideX, l.y + 209, l.sideW, 22, "선택 땅 해제", mouseX, mouseY, selected >= 0);
        String legend = notice.isEmpty() ? "청록 다이아: 내 땅  ·  금색 십자: 내 위치  ·  방향키 이동" : notice;
        g.drawString(font, trim(legend, l.w - 24), l.x + 12, l.y + l.h - 12, notice.isEmpty() ? 0xFF9CAFC5 : 0xFFFFC86A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    public void notice(String message) {
        this.notice = message;
    }

    private String trim(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        while (!value.isEmpty() && font.width(value + "...") > maxWidth) value = value.substring(0, value.length() - 1);
        return value + "...";
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        button(g, x, y, w, h, label, mx, my, true);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean enabled) {
        boolean hover = enabled && inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, !enabled ? 0xFF3A4654 : hover ? 0xFF47D6CE : 0xFF496782);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, !enabled ? 0xFF273341 : hover ? 0xFF385367 : 0xFF294052);
        g.drawCenteredString(font, label, x + w / 2, y + 7, enabled ? 0xFFF5F7FB : 0xFF8294A5);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        Layout l = layout();
        if (inside(mx, my, l.mapX, l.mapY, GRID, GRID)) {
            centerX += (int)(mx - l.mapX) / CELL - CELLS / 2;
            centerZ += (int)(my - l.mapY) / CELL - CELLS / 2;
            return true;
        }
        int half = (l.sideW - 6) / 2;
        if (inside(mx, my, l.sideX, l.y + 155, half, 22)) focus(-1);
        else if (inside(mx, my, l.sideX + half + 6, l.y + 155, half, 22)) focus(1);
        else if (inside(mx, my, l.sideX, l.y + 182, half, 22)) {
            if (minecraft.player != null) {
                centerX = minecraft.player.chunkPosition().x;
                centerZ = minecraft.player.chunkPosition().z;
            }
        } else if (inside(mx, my, l.sideX + half + 6, l.y + 182, half, 22)) onClose();
        else if (inside(mx, my, l.sideX, l.y + 209, l.sideW, 22) && selected >= 0) confirmRelease(claims.get(selected));
        else return super.mouseClicked(mx, my, button);
        return true;
    }

    private void confirmRelease(EstateClaimsPayload.Chunk chunk) {
        Screen previous = this;
        String dimension = ClientClaimMap.dimension();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            minecraft.setScreen(previous);
            if (confirmed) {
                PacketDistributor.sendToServer(new EstateClaimActionPayload("release_at", dimension, chunk.x(), chunk.z(), ""));
                notice = "서버에서 처리하는 중...";
            }
        }, Component.literal("소유권 해제"),
                Component.literal("[" + chunk.x() + ", " + chunk.z() + "] 청크의 소유권을 해제하시겠습니까? 환불되지 않으며 되돌릴 수 없습니다.")));
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

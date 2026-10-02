package kr.mokea.msr.estate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.NumberFormat;
import java.util.Locale;

public final class EstateScreen extends Screen {
    private static final int BG = 0xF017202D;
    private static final int CARD = 0xFF253247;
    private static final int CYAN = 0xFF47D6CE;
    private static final int WHITE = 0xFFF5F7FB;
    private static final int MUTED = 0xFF9CAFC5;
    private static final int GOLD = 0xFFFFC86A;
    private EstateStatePayload state;
    private String notice = "판매 버튼은 인벤토리의 해당 주괴를 모두 판매합니다.";
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private long releaseArmedUntil;

    public EstateScreen() {
        super(Component.literal("MSR Estates"));
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.player != null) {
            lastChunkX = minecraft.player.chunkPosition().x;
            lastChunkZ = minecraft.player.chunkPosition().z;
        }
        request("status");
    }

    public void accept(EstateStatePayload next) {
        state = next;
        lastChunkX = next.chunkX();
        lastChunkZ = next.chunkZ();
        if (!next.message().isEmpty()) notice = next.message();
        else notice = "판매 버튼은 인벤토리의 해당 주괴를 모두 판매합니다.";
    }

    @Override
    public void tick() {
        if (minecraft != null && minecraft.player != null) {
            int x = minecraft.player.chunkPosition().x;
            int z = minecraft.player.chunkPosition().z;
            if (x != lastChunkX || z != lastChunkZ) {
                lastChunkX = x;
                lastChunkZ = z;
                request("status");
            }
        }
    }

    private void request(String action) {
        if (minecraft != null && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new EstateActionPayload(action));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBlurredBackground(partialTick);
        g.fill(0, 0, width, height, 0x2607121D);
        int w = Math.min(390, width - 18);
        int h = Math.min(226, height - 10);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        panel(g, x, y, w, h, BG, CYAN);
        g.fillGradient(x + 2, y + 2, x + w - 2, y + 33, 0xFF25384B, 0xFF1D2A3B);
        g.fill(x + 12, y + 12, x + 16, y + 23, CYAN);
        g.drawString(font, "MSR  /  ESTATES", x + 23, y + 14, WHITE);
        g.drawString(font, "G", x + w - 27, y + 14, MUTED);

        int inner = w - 24;
        int col = (inner - 8) / 2;
        int top = y + 40;
        panel(g, x + 12, top, col, 46, CARD, 0xFF3F526B);
        panel(g, x + 20 + col, top, col, 46, CARD, 0xFF3F526B);
        g.drawString(font, "BALANCE", x + 22, top + 9, MUTED);
        String money = state == null ? "--" : NumberFormat.getNumberInstance(Locale.KOREA).format(state.balance()) + "원";
        g.drawString(font, money, x + 22, top + 26, GOLD);
        g.drawString(font, "MY LAND", x + 30 + col, top + 9, MUTED);
        g.drawString(font, state == null ? "--" : state.claimCount() + " chunks", x + 30 + col, top + 26, CYAN);

        int mapY = top + 53;
        panel(g, x + 12, mapY, inner, 48, 0xFF1C2A3B, 0xFF32465E);
        drawMap(g, x + 20, mapY + 9);
        int infoX = x + 79;
        g.drawString(font, "CURRENT CHUNK", infoX, mapY + 7, MUTED);
        String location = state == null ? "loading..." : state.dimension() + "  [" + state.chunkX() + ", " + state.chunkZ() + "]";
        g.drawString(font, trim(location, w - 105), infoX, mapY + 21, WHITE);
        String owner = state == null ? "정보를 불러오는 중" : state.ownerName().isEmpty() ? "구매 가능  ·  100,000원"
                : state.ownedByYou() ? "내 소유 청크" : "소유자: " + state.ownerName();
        g.drawString(font, trim(owner, w - 105), infoX, mapY + 35, state == null ? MUTED : state.ownerName().isEmpty() ? GOLD : CYAN);

        int actionY = mapY + 55;
        boolean unclaimed = state != null && state.ownerName().isEmpty();
        boolean mine = state != null && state.ownedByYou();
        drawButton(g, x + 12, actionY, (inner - 8) / 2, 25, unclaimed ? "청크 구매 · 100,000원" : mine ? (System.currentTimeMillis() < releaseArmedUntil ? "다시 눌러 해제 확인" : "소유권 해제") : "구매 불가",
                mouseX, mouseY, (unclaimed || mine) ? CYAN : 0xFF506174, unclaimed || mine);
        drawButton(g, x + 20 + (inner - 8) / 2, actionY, (inner - 8) / 2, 25, "소유 청크 지도",
                mouseX, mouseY, 0xFF5A91BC, true);

        int sellY = actionY + 31;
        int sellW = (inner - 12) / 3;
        drawButton(g, x + 12, sellY, sellW, 25, "철 ×" + (state == null ? 0 : state.ironCount()) + " · 1,000원", mouseX, mouseY, 0xFF94AFC6, true);
        drawButton(g, x + 18 + sellW, sellY, sellW, 25, "금 ×" + (state == null ? 0 : state.goldCount()) + " · 5,000원", mouseX, mouseY, GOLD, true);
        drawButton(g, x + 24 + sellW * 2, sellY, sellW, 25, "네더 ×" + (state == null ? 0 : state.netheriteCount()) + " · 10만원", mouseX, mouseY, 0xFFBD8ADD, true);

        g.drawString(font, trim(notice, inner), x + 13, y + h - 17, MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private String trim(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        while (!value.isEmpty() && font.width(value + "...") > maxWidth) value = value.substring(0, value.length() - 1);
        return value + "...";
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void drawMap(GuiGraphics g, int x, int y) {
        int cx = state == null ? 0 : state.chunkX();
        int cz = state == null ? 0 : state.chunkZ();
        String dimension = state == null ? "" : state.dimension();
        EstateClientVisuals.drawGrid(g, x + 2, y + 4, 5, 7, dimension, cx, cz);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label,
                            int mouseX, int mouseY, int accent, boolean enabled) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, w, h);
        int background = enabled ? (hover ? 0xFF385367 : 0xFF294052) : 0xFF273341;
        panel(g, x, y, w, h, background, hover ? accent : 0xFF41576A);
        g.fill(x + 2, y + 2, x + 5, y + h - 2, accent);
        g.drawCenteredString(font, trim(label, w - 12), x + w / 2 + 2, y + 8, enabled ? WHITE : MUTED);
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
        int inner = w - 24;
        int actionY = y + 40 + 53 + 55;
        int half = (inner - 8) / 2;
        int sellY = actionY + 31;
        int sellW = (inner - 12) / 3;
        if (inside(mx, my, x + 12, actionY, half, 25) && state != null) {
            if (state.ownerName().isEmpty()) request("buy");
            else if (state.ownedByYou()) {
                if (System.currentTimeMillis() < releaseArmedUntil) {
                    releaseArmedUntil = 0;
                    request("release");
                } else {
                    releaseArmedUntil = System.currentTimeMillis() + 5000;
                    notice = "소유권 해제는 환불되지 않습니다. 5초 안에 다시 누르세요.";
                    return true;
                }
            }
        } else if (inside(mx, my, x + 20 + half, actionY, half, 25)) minecraft.setScreen(new EstateMapScreen(this));
        else if (inside(mx, my, x + 12, sellY, sellW, 25)) request("sell_iron");
        else if (inside(mx, my, x + 18 + sellW, sellY, sellW, 25)) request("sell_gold");
        else if (inside(mx, my, x + 24 + sellW * 2, sellY, sellW, 25)) request("sell_netherite");
        else return super.mouseClicked(mx, my, button);
        notice = "서버에서 처리하는 중...";
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

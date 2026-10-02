package kr.mokea.msr.estate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Lists every chunk the player owns with a rename and a release action, independent of where they're standing. */
public final class EstateManageScreen extends Screen {
    private static final int ROWS_PER_PAGE = 6;
    private static final int ROW_H = 30;

    private final Screen previous;
    private int page;
    private String notice = "";
    private List<EstateClaimsPayload.Chunk> claims = List.of();

    public EstateManageScreen(Screen previous) {
        super(Component.literal("MSR Manage Claims"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        refresh();
    }

    @Override
    public void tick() {
        refresh();
    }

    private void refresh() {
        claims = ClientClaimMap.chunks();
        int maxPage = Math.max(0, (claims.size() - 1) / ROWS_PER_PAGE);
        if (page > maxPage) page = maxPage;
    }

    public void notice(String message) {
        this.notice = message;
    }

    private record Layout(int x, int y, int w, int h, int listX, int listY, int listW) {}

    private Layout layout() {
        int w = Math.min(420, width - 18);
        int h = Math.min(260, height - 10);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        return new Layout(x, y, w, h, x + 12, y + 40, w - 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0x4207121D); // Keep the world sharp behind the dashboard.
        g.fill(l.x, l.y, l.x + l.w, l.y + l.h, 0xFF47D6CE);
        g.fill(l.x + 1, l.y + 1, l.x + l.w - 1, l.y + l.h - 1, 0xF017202D);
        g.fillGradient(l.x + 2, l.y + 2, l.x + l.w - 2, l.y + 33, 0xFF25384B, 0xFF1D2A3B);
        g.drawString(font, "MSR  /  청크 관리", l.x + 17, l.y + 14, 0xFFF5F7FB);

        if (claims.isEmpty()) {
            g.drawCenteredString(font, "소유한 청크가 없습니다.", l.x + l.w / 2, l.listY + 20, 0xFF9CAFC5);
        }
        int start = page * ROWS_PER_PAGE;
        for (int i = 0; i < ROWS_PER_PAGE && start + i < claims.size(); i++) {
            EstateClaimsPayload.Chunk chunk = claims.get(start + i);
            int rowY = l.listY + i * ROW_H;
            g.fill(l.listX, rowY, l.listX + l.listW, rowY + ROW_H - 4, 0xFF1C2A3B);
            String label = chunk.name().isEmpty() ? "이름 없는 땅" : chunk.name();
            g.drawString(font, trim(label, 150), l.listX + 8, rowY + 4, chunk.name().isEmpty() ? 0xFF8294A5 : 0xFFF5F7FB);
            g.drawString(font, "[" + chunk.x() + ", " + chunk.z() + "]", l.listX + 8, rowY + 15, 0xFF8EA6BD);
            int btnW = 64;
            rowButton(g, l.listX + l.listW - btnW * 2 - 8, rowY + 2, btnW, 20, "이름 변경", mouseX, mouseY, 0xFF5A91BC);
            rowButton(g, l.listX + l.listW - btnW, rowY + 2, btnW, 20, "해제", mouseX, mouseY, 0xFFE0796E);
        }

        int footerY = l.y + l.h - 46;
        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) * ROWS_PER_PAGE < claims.size();
        rowButton(g, l.x + 12, footerY, 80, 20, "이전 페이지", mouseX, mouseY, hasPrev ? 0xFF5A91BC : 0xFF3A4654);
        g.drawCenteredString(font, (page + 1) + " / " + Math.max(1, (claims.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE),
                l.x + l.w / 2, footerY + 6, 0xFF9CAFC5);
        rowButton(g, l.x + l.w - 92, footerY, 80, 20, "다음 페이지", mouseX, mouseY, hasNext ? 0xFF5A91BC : 0xFF3A4654);
        rowButton(g, l.x + l.w / 2 - 40, footerY, 80, 20, "돌아가기", mouseX, mouseY, 0xFF8AA6C2);

        String legend = notice.isEmpty() ? "청크 이름을 지정하거나, 멀리서도 소유권을 해제할 수 있습니다." : notice;
        g.drawString(font, trim(legend, l.w - 24), l.x + 12, l.y + l.h - 14, notice.isEmpty() ? 0xFF9CAFC5 : 0xFFFFC86A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void rowButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, int accent) {
        boolean hover = inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? accent : 0xFF294052);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFF385367 : 0xFF1C2A3B);
        g.drawCenteredString(font, label, x + w / 2, y + 6, 0xFFF5F7FB);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trim(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        while (!value.isEmpty() && font.width(value + "...") > maxWidth) value = value.substring(0, value.length() - 1);
        return value + "...";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        Layout l = layout();
        int start = page * ROWS_PER_PAGE;
        for (int i = 0; i < ROWS_PER_PAGE && start + i < claims.size(); i++) {
            EstateClaimsPayload.Chunk chunk = claims.get(start + i);
            int rowY = l.listY + i * ROW_H;
            int btnW = 64;
            if (inside(mx, my, l.listX + l.listW - btnW * 2 - 8, rowY + 2, btnW, 20)) {
                minecraft.setScreen(new EstateRenameScreen(this, chunk));
                return true;
            }
            if (inside(mx, my, l.listX + l.listW - btnW, rowY + 2, btnW, 20)) {
                confirmRelease(chunk);
                return true;
            }
        }
        int footerY = l.y + l.h - 46;
        if (inside(mx, my, l.x + 12, footerY, 80, 20) && page > 0) page--;
        else if (inside(mx, my, l.x + l.w - 92, footerY, 80, 20) && (page + 1) * ROWS_PER_PAGE < claims.size()) page++;
        else if (inside(mx, my, l.x + l.w / 2 - 40, footerY, 80, 20)) onClose();
        else return super.mouseClicked(mx, my, button);
        return true;
    }

    private void confirmRelease(EstateClaimsPayload.Chunk chunk) {
        Screen previousScreen = this;
        String dimension = ClientClaimMap.dimension();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            minecraft.setScreen(previousScreen);
            if (confirmed) {
                PacketDistributor.sendToServer(new EstateClaimActionPayload("release_at", dimension, chunk.x(), chunk.z(), ""));
                notice = "서버에서 처리하는 중...";
            }
        }, Component.literal("소유권 해제"),
                Component.literal("[" + chunk.x() + ", " + chunk.z() + "] 청크의 소유권을 해제하시겠습니까? 환불되지 않으며 되돌릴 수 없습니다.")));
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

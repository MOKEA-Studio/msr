package kr.mokea.msr.estate;

import kr.mokea.msr.MsrMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class EstateClientVisuals {
    private static final int FOG = 0xFF131D29;
    private static final int PENDING_OWNED = 0xFF27424A;
    private static final int OWN_MARK = 0xFF47D6CE;
    private static final int PLAYER_MARK = 0xFFFFC86A;
    private static final int FRAME_LIGHT = 0xFF6C87A3;
    private static final int FRAME_DARK = 0xFF1C2635;
    private static final int COMPASS_BORDER = 0xFF3F6E8C;
    private static final int COMPASS_TEXT = 0xFF9FE8FF;

    private static final Map<Long, Integer> terrainCache = new HashMap<>();
    private static String terrainDimension = "";

    private static int ticks;
    private static boolean requestedInitialState;
    private EstateClientVisuals() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ClientClaimMap.clear();
            ticks = 0;
            requestedInitialState = false;
            return;
        }
        if (!requestedInitialState && mc.getConnection() != null) {
            requestedInitialState = true;
            PacketDistributor.sendToServer(new EstateActionPayload("status"));
        }
        if (++ticks % 10 != 0 || mc.isPaused()) return;
        String dimension = mc.level.dimension().location().toString();
        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;
        double y = mc.player.getY() + 0.65;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                if (!ClientClaimMap.owns(dimension, chunkX, chunkZ)) continue;
                if (Math.abs(chunkX * 16 + 8 - mc.player.getX()) > 24
                        || Math.abs(chunkZ * 16 + 8 - mc.player.getZ()) > 24) continue;
                double x0 = chunkX * 16 + 0.5;
                double z0 = chunkZ * 16 + 0.5;
                for (int offset : new int[] {2, 6, 10, 14}) {
                    particle(mc, x0 + offset, y, z0);
                    particle(mc, x0 + offset, y, z0 + 15);
                    particle(mc, x0, y, z0 + offset);
                    particle(mc, x0 + 15, y, z0 + offset);
                }
            }
        }
    }

    private static void particle(Minecraft mc, double x, double y, double z) {
        mc.level.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.02, 0);
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        String dimension = mc.level.dimension().location().toString();
        if (!dimension.equals(ClientClaimMap.dimension())) return;
        int cx = mc.player.chunkPosition().x;
        int cz = mc.player.chunkPosition().z;
        int x = g.guiWidth() - 83;
        int y = 8;
        g.fill(x, y, x + 75, y + 75, 0xC0111B29);
        g.fill(x, y, x + 75, y + 1, 0xFF47D6CE);
        g.drawCenteredString(mc.font, "MY CHUNKS", x + 37, y + 5, 0xFFEAF5F6);
        drawGrid(g, x + 14, y + 18, 9, 5, dimension, cx, cz, cx, cz, false);
        g.drawCenteredString(mc.font, cx + ", " + cz, x + 37, y + 65, 0xFFB6C8D9);
    }

    public static void drawGrid(GuiGraphics g, int x, int y, int cells, int cellSize,
                                String dimension, int centerX, int centerZ) {
        drawGrid(g, x, y, cells, cellSize, dimension, centerX, centerZ, centerX, centerZ, false);
    }

    public static void drawGrid(GuiGraphics g, int x, int y, int cells, int cellSize,
                                String dimension, int centerX, int centerZ, int playerX, int playerZ, boolean compass) {
        Minecraft mc = Minecraft.getInstance();
        int radius = cells / 2;
        for (int row = 0; row < cells; row++) {
            for (int col = 0; col < cells; col++) {
                int cx = centerX + col - radius;
                int cz = centerZ + row - radius;
                int colorOwned = ClientClaimMap.colorOf(dimension, cx, cz);
                boolean owned = colorOwned != 0 || ClientClaimMap.owns(dimension, cx, cz);
                int left = x + col * cellSize;
                int top = y + row * cellSize;
                int background = owned ? (colorOwned != 0 ? colorOwned : PENDING_OWNED) : terrainColor(mc, dimension, cx, cz);
                g.fill(left, top, left + cellSize - 1, top + cellSize - 1, background);
                if (owned) drawDiamond(g, left, top, cellSize, OWN_MARK);
                if (cx == playerX && cz == playerZ) drawCross(g, left, top, cellSize, PLAYER_MARK);
            }
        }
        if (compass) drawFrameAndCompass(g, x, y, cells * cellSize, cells * cellSize);
    }

    private static int terrainColor(Minecraft mc, String dimension, int chunkX, int chunkZ) {
        if (!dimension.equals(terrainDimension)) {
            terrainCache.clear();
            terrainDimension = dimension;
        }
        long key = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
        Integer cached = terrainCache.get(key);
        if (cached != null) return cached;
        if (mc.level == null || !dimension.equals(mc.level.dimension().location().toString())) return FOG;
        int color = TerrainColorSampler.sampleChunkColor(mc.level, chunkX, chunkZ);
        if (color == 0) return FOG;
        terrainCache.put(key, color);
        return color;
    }

    private static void drawDiamond(GuiGraphics g, int left, int top, int size, int color) {
        int cx = left + size / 2;
        int cy = top + size / 2;
        int r = Math.max(1, size / 2 - 1);
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            if (half < 0) continue;
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void drawCross(GuiGraphics g, int left, int top, int size, int color) {
        int cx = left + size / 2;
        int cy = top + size / 2;
        int arm = Math.max(2, size / 2);
        g.fill(cx - arm, cy, cx + arm + 1, cy + 1, color);
        g.fill(cx, cy - arm, cx + 1, cy + arm + 1, color);
    }

    private static void drawFrameAndCompass(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 3, x + w + 3, y, FRAME_LIGHT);
        g.fill(x - 3, y + h, x + w + 3, y + h + 3, FRAME_LIGHT);
        g.fill(x - 3, y, x, y + h, FRAME_LIGHT);
        g.fill(x + w, y, x + w + 3, y + h, FRAME_LIGHT);
        g.fill(x - 2, y - 2, x + w + 2, y - 1, FRAME_DARK);
        g.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, FRAME_DARK);
        g.fill(x - 2, y - 1, x - 1, y + h + 1, FRAME_DARK);
        g.fill(x + w + 1, y - 1, x + w + 2, y + h + 1, FRAME_DARK);
        compassTag(g, x + w / 2 - 6, y - 15, "N");
        compassTag(g, x + w / 2 - 6, y + h + 4, "S");
        compassTag(g, x - 18, y + h / 2 - 5, "W");
        compassTag(g, x + w + 5, y + h / 2 - 5, "E");
    }

    private static void compassTag(GuiGraphics g, int x, int y, String label) {
        Minecraft mc = Minecraft.getInstance();
        g.fill(x, y, x + 13, y + 11, COMPASS_BORDER);
        g.fill(x + 1, y + 1, x + 12, y + 10, FRAME_DARK);
        g.drawCenteredString(mc.font, label, x + 7, y + 2, COMPASS_TEXT);
    }
}

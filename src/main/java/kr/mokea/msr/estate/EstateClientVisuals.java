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

@EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class EstateClientVisuals {
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
        drawGrid(g, x + 14, y + 18, 9, 5, dimension, cx, cz);
        g.drawCenteredString(mc.font, cx + ", " + cz, x + 37, y + 65, 0xFFB6C8D9);
    }

    public static void drawGrid(GuiGraphics g, int x, int y, int cells, int cellSize,
                                String dimension, int centerX, int centerZ) {
        drawGrid(g, x, y, cells, cellSize, dimension, centerX, centerZ, centerX, centerZ);
    }

    public static void drawGrid(GuiGraphics g, int x, int y, int cells, int cellSize,
                                String dimension, int centerX, int centerZ, int playerX, int playerZ) {
        int radius = cells / 2;
        for (int row = 0; row < cells; row++) {
            for (int col = 0; col < cells; col++) {
                int cx = centerX + col - radius;
                int cz = centerZ + row - radius;
                boolean owned = ClientClaimMap.owns(dimension, cx, cz);
                boolean player = cx == playerX && cz == playerZ;
                int color = player ? 0xFFFFC86A : owned ? 0xFF47D6CE : 0xFF34465A;
                int left = x + col * cellSize;
                int top = y + row * cellSize;
                g.fill(left, top, left + cellSize - 1, top + cellSize - 1, color);
                if (player && owned && cellSize >= 5) g.fill(left + 1, top + 1, left + cellSize - 2, top + cellSize - 2, 0xFF47D6CE);
            }
        }
    }
}

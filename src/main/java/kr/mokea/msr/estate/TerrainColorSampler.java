package kr.mokea.msr.estate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;

/** Approximates a chunk's surface color the way vanilla maps do, at chunk (not per-block) resolution. */
public final class TerrainColorSampler {
    private static final int STEP = 4;

    private TerrainColorSampler() {}

    /** Returns a packed ARGB color, or 0 if the chunk is not currently loaded. */
    public static int sampleChunkColor(Level level, int chunkX, int chunkZ) {
        if (!level.hasChunk(chunkX, chunkZ)) return 0;
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Map<MapColor, Integer> counts = new HashMap<>();
        long northHeight = 0, southHeight = 0;
        int northSamples = 0, southSamples = 0;

        for (int dz = 0; dz < 16; dz += STEP) {
            for (int dx = 0; dx < 16; dx += STEP) {
                int x = originX + dx;
                int z = originZ + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                MapColor color = MapColor.NONE;
                while (y > minY) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    color = state.getMapColor(level, pos);
                    if (color != MapColor.NONE) break;
                    y--;
                }
                if (color == MapColor.NONE) color = MapColor.STONE;
                counts.merge(color, 1, Integer::sum);
                if (dz < 8) {
                    northHeight += y;
                    northSamples++;
                } else {
                    southHeight += y;
                    southSamples++;
                }
            }
        }

        MapColor dominant = MapColor.STONE;
        int best = -1;
        for (Map.Entry<MapColor, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                dominant = entry.getKey();
            }
        }

        MapColor.Brightness brightness;
        if (dominant == MapColor.WATER) {
            brightness = MapColor.Brightness.NORMAL;
        } else {
            double north = northSamples == 0 ? 0 : (double) northHeight / northSamples;
            double south = southSamples == 0 ? 0 : (double) southHeight / southSamples;
            double delta = south - north;
            brightness = delta > 1.0 ? MapColor.Brightness.HIGH : delta < -1.0 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        }
        return 0xFF000000 | dominant.calculateRGBColor(brightness);
    }
}

package kr.mokea.msr.estate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Soft integration with Xaero's Minimap: marks owned chunks as waypoints on its real map.
 * Xaero has no public mod API, so this reflects into its internal (unversioned) classes.
 * Any failure permanently disables the bridge for the session instead of crashing or spamming logs.
 */
public final class XaeroBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroBridge.class);
    private static final String SYMBOL = "MSR";
    private static final int WAYPOINT_Y = 64;

    private static boolean checked;
    private static boolean available;

    private static Method getCurrentSession;
    private static Method getWaypointsManager;
    private static Method getCurrentWorld;
    private static Method getDimId;
    private static Method getCurrentSet;
    private static Method getList;
    private static Method getX;
    private static Method getZ;
    private static Method getSymbol;
    private static Constructor<?> waypointConstructor;
    private static Object aquaColor;

    private XaeroBridge() {}

    public static void sync() {
        if (!ready()) return;
        try {
            syncUnsafe();
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Xaero's own internals can throw transiently (e.g. mid-disconnect); skip this attempt only.
            LOGGER.debug("Skipped an Xaero's Minimap waypoint sync", e);
        }
    }

    private static boolean ready() {
        if (checked) return available;
        checked = true;
        try {
            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Class<?> managerClass = Class.forName("xaero.common.minimap.waypoints.WaypointsManager");
            Class<?> worldClass = Class.forName("xaero.common.minimap.waypoints.WaypointWorld");
            Class<?> setClass = Class.forName("xaero.common.minimap.waypoints.WaypointSet");
            Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Class<?> colorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");

            getCurrentSession = sessionClass.getMethod("getCurrentSession");
            getWaypointsManager = sessionClass.getMethod("getWaypointsManager");
            getCurrentWorld = managerClass.getMethod("getCurrentWorld");
            getDimId = worldClass.getMethod("getDimId");
            getCurrentSet = worldClass.getMethod("getCurrentSet");
            getList = setClass.getMethod("getList");
            getX = waypointClass.getMethod("getX");
            getZ = waypointClass.getMethod("getZ");
            getSymbol = waypointClass.getMethod("getSymbol");
            waypointConstructor = waypointClass.getConstructor(
                    int.class, int.class, int.class, String.class, String.class, colorClass);
            aquaColor = colorClass.getField("AQUA").get(null);

            available = true;
            LOGGER.info("Xaero's Minimap detected; MSR claims will appear as waypoints");
        } catch (ReflectiveOperationException | LinkageError e) {
            available = false;
        }
        return available;
    }

    @SuppressWarnings("unchecked")
    private static void syncUnsafe() throws ReflectiveOperationException {
        Object session = getCurrentSession.invoke(null);
        if (session == null) return;
        Object manager = getWaypointsManager.invoke(session);
        if (manager == null) return;
        Object world = getCurrentWorld.invoke(manager);
        if (world == null) return;

        ResourceKey<Level> dimKey = (ResourceKey<Level>) getDimId.invoke(world);
        String dimension = ClientClaimMap.dimension();
        if (dimKey == null || !dimKey.location().toString().equals(dimension)) return;

        Object set = getCurrentSet.invoke(world);
        if (set == null) return;
        List<Object> waypoints = (List<Object>) getList.invoke(set);

        Set<Long> owned = new HashSet<>();
        for (EstateClaimsPayload.Chunk chunk : ClientClaimMap.chunks()) owned.add(chunkKey(chunk.x(), chunk.z()));

        Set<Long> present = new HashSet<>();
        for (Object waypoint : List.copyOf(waypoints)) {
            if (!SYMBOL.equals(getSymbol.invoke(waypoint))) continue;
            long key = chunkKey(((int) getX.invoke(waypoint)) >> 4, ((int) getZ.invoke(waypoint)) >> 4);
            if (owned.contains(key)) present.add(key);
            else waypoints.remove(waypoint);
        }

        for (EstateClaimsPayload.Chunk chunk : ClientClaimMap.chunks()) {
            long key = chunkKey(chunk.x(), chunk.z());
            if (present.contains(key)) continue;
            int blockX = chunk.x() * 16 + 8;
            int blockZ = chunk.z() * 16 + 8;
            waypoints.add(waypointConstructor.newInstance(
                    blockX, WAYPOINT_Y, blockZ, "MSR 소유 청크", SYMBOL, aquaColor));
        }
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}

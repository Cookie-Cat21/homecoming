package dev.homecoming.client;

import dev.homecoming.Journal;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;

/** Version-pinned adapter for Xaero's Minimap 26.6.0 on MC 1.21.1. */
final class XaeroIntegration {
    static String track(long id, String label, Journal.Place place) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().toString().equals(place.dimension()))
            return "Enter " + place.dimension() + " before tracking this location.";
        var session = XaeroMinimapSession.getCurrentSession();
        if (session == null) return "Xaero's map is still loading. Try again shortly.";
        var manager = session.getWaypointsManager();
        var world = manager.getWorldManager().getCurrentWorld();
        if (world == null || !world.getDimId().location().toString().equals(place.dimension()))
            return "Map world is not ready for this dimension.";
        var set = world.getWaypointSet("Homecoming Shared");
        if (set == null) { world.addWaypointSet("Homecoming Shared"); set = world.getWaypointSet("Homecoming Shared"); }
        var origin = ResourceLocation.fromNamespaceAndPath("homecoming", "shared");
        Waypoint existing = null;
        for (var waypoint : set.getWaypoints())
            // Xaero's disk format does not retain thirdPartyOrigin. Match the persistent
            // symbol and coordinates inside our set so tracking after a restart is idempotent.
            if (waypoint.getSymbol().equals("H" + id) && waypoint.getX() == place.x()
                    && waypoint.getY() == place.y() && waypoint.getZ() == place.z()) { existing = waypoint; break; }
        if (existing == null) {
            var waypoint = new Waypoint(place.x(), place.y(), place.z(), label, "H" + id, WaypointColor.GREEN);
            waypoint.setThirdPartyOrigin(origin); set.add(waypoint);
        }
        world.setCurrentWaypointSetId("Homecoming Shared");
        try { manager.getWorldManagerIO().saveWorld(world); }
        catch (java.io.IOException error) { return "Waypoint added for this session; saving failed."; }
        return "Waypoint added to Homecoming Shared.";
    }
    private XaeroIntegration() {}
}

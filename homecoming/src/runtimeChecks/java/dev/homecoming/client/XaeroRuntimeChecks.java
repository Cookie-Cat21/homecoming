package dev.homecoming.client;

import dev.homecoming.Journal;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import java.util.ArrayList;

/** Explicit opt-in, real-client map checks in a disposable development world. */
@EventBusSubscriber(modid="homecoming", value=Dist.CLIENT)
public final class XaeroRuntimeChecks {
    private static boolean finished;
    private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (finished || !Boolean.getBoolean("homecoming.clientChecks")) return;
        var mc=Minecraft.getInstance();
        if (mc.level==null || mc.player==null || ++ticks < 100) return;
        var session=XaeroMinimapSession.getCurrentSession();
        if (session==null) return;
        var world=session.getWaypointsManager().getWorldManager().getCurrentWorld();
        if (world==null || !world.getDimId().equals(mc.level.dimension())) return;
        finished=true;
        try {
            String personalName="Homecoming Test Personal";
            var personal=world.getWaypointSet(personalName);
            if (personal==null) { world.addWaypointSet(personalName); personal=world.getWaypointSet(personalName); }
            if (snapshot(personal.getWaypoints()).isEmpty()) personal.add(new Waypoint(123,70,456,"Personal test marker","P",WaypointColor.RED));
            var before=fingerprint(personal.getWaypoints());
            var dimension=mc.level.dimension().location().toString();
            var shared=world.getWaypointSet("Homecoming Shared");
            boolean persisted=shared!=null && snapshot(shared.getWaypoints()).stream().anyMatch(w -> w.getSymbol().equals("H90000000"));
            var place=new Journal.Place(dimension,42,80,-37);
            String result=XaeroIntegration.track(90000000L,"Homecoming map regression",place);
            require(result.equals("Waypoint added to Homecoming Shared."),result);
            shared=world.getWaypointSet("Homecoming Shared");
            int count=snapshot(shared.getWaypoints()).size();
            XaeroIntegration.track(90000000L,"Homecoming map regression",place);
            require(snapshot(shared.getWaypoints()).size()==count,"Duplicate tracking created another waypoint");
            require(snapshot(shared.getWaypoints()).stream().filter(w -> w.getSymbol().equals("H90000000")).count()==1,"Restart tracking duplicated saved waypoint");
            require(fingerprint(personal.getWaypoints()).equals(before),"Personal set changed");
            String wrongDimension=dimension.equals("minecraft:the_nether")?"minecraft:overworld":"minecraft:the_nether";
            require(XaeroIntegration.track(90000001L,"Wrong dimension",new Journal.Place(wrongDimension,5,70,5)).startsWith("Enter "),"Cross-dimension location was accepted");
            require(snapshot(shared.getWaypoints()).size()==count,"Cross-dimension tracking mutated the active map");
            String resultLine="HOMECOMING_XAERO_RUNTIME_VERIFIED dimension="+dimension+" persisted="+persisted+" personalMarkers="+before.size();
            java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("homecoming-xaero-result.txt"),resultLine);
            System.out.println(resultLine);
        } catch (Throwable failure) {
            System.err.println("HOMECOMING_XAERO_RUNTIME_FAILED "+failure);
            failure.printStackTrace();
        } finally {
            mc.stop();
        }
    }
    private static ArrayList<Waypoint> snapshot(Iterable<Waypoint> values) {
        var result=new ArrayList<Waypoint>(); values.forEach(result::add); return result;
    }
    private static java.util.List<String> fingerprint(Iterable<Waypoint> values) {
        return snapshot(values).stream().map(w -> w.getName()+"|"+w.getSymbol()+"|"+w.getX()+"|"+w.getY()+"|"+w.getZ()).toList();
    }
    private static void require(boolean condition,String message) { if (!condition) throw new IllegalStateException(message); }
}

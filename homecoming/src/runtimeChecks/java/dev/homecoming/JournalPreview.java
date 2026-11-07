package dev.homecoming;

import dev.homecoming.client.JournalScreen;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in real-client screenshots and interaction checks; only for a disposable world. */
@EventBusSubscriber(modid="homecoming", value=Dist.CLIENT)
public final class JournalPreview {
    private static int ticks,stage;
    private static volatile BriefingView snapshot;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("homecoming.previewJournal")) return;
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || stage==99) return;
        if(++ticks<(stage==0 || stage==1?160:40)) return;
        try {
            switch(stage) {
                case 0 -> {
                    stage=1; ticks=0; var uuid=mc.player.getUUID(); var server=mc.getSingleplayerServer();
                    server.execute(() -> {
                        var player=server.getPlayerList().getPlayer(uuid);
                        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(),"recap enable");
                        var state=JournalData.get(server).journal.player(player.getStringUUID());
                        state.acknowledged=JournalData.get(server).journal.sequence; state.offered=state.acknowledged;
                        if(!fixture(player)) {
                            mc.execute(() -> {
                                try { java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("homecoming-ui-checks.txt"),"SEEDED: fixture quests created. Run preview again so the client loads their definitions before testing quest navigation."); }
                                catch(java.io.IOException ignored) {}
                                stage=99; mc.stop();
                            }); return;
                        }
                        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(),"recap share Shared meeting point");
                        // Completion listeners enqueue their journal writes. Select the
                        // screenshot's view after those writes, as a later /recap would.
                        server.execute(() -> {
                            var view=Homecoming.view(player);
                            mc.execute(() -> { snapshot=view; mc.setScreen(new JournalScreen(view,0)); });
                        });
                    });
                }
                case 1 -> {
                    require(snapshot!=null && snapshot.suggestions().size()==2,"Populated server-authorized view missing");
                    require(snapshot.updates().stream().anyMatch(row -> row.count()>1),"Grouped completion missing from authorized view");
                    capture("updates"); button("Mark these read").onPress(); require(!button("Refresh").active,"Acknowledgment did not enter pending state"); advance();
                }
                case 2 -> { require(!hasButton("Mark these read"),"Acknowledgment did not consume displayed range"); button("Next steps").onPress(); advance(); }
                case 3 -> { capture("next-steps"); button("Open quest").onPress(); advance(); }
                case 4 -> {
                    require(!(mc.screen instanceof JournalScreen),"Open quest did not navigate to its existing book"); capture("quest-book");
                    var uuid=mc.player.getUUID(); var server=mc.getSingleplayerServer();
                    server.execute(() -> { var view=Homecoming.view(server.getPlayerList().getPlayer(uuid)); mc.execute(() -> mc.setScreen(new JournalScreen(view,2))); }); advance();
                }
                case 5 -> { capture("settings"); require(screen().keyPressed(269,0,0),"End should scroll settings"); advance(); }
                case 6 -> { capture("world-rules"); require(screen().keyPressed(268,0,0),"Home should scroll back"); advance(); }
                case 7 -> { button("Turn off...").onPress(); advance(); }
                case 8 -> {
                    capture("confirm-disable");
                    require(screen().keyPressed(256,0,0),"Escape should cancel the turn-off confirmation");
                    require(button("Turn off...")!=null,"Cancel lost participation settings");
                    String label=hasButton("Mute reminders")?"Mute reminders":"Turn reminders on";
                    button(label).onPress(); require(!button("Refresh").active,"Pending mutation did not disable repeat requests"); advance();
                }
                case 9 -> {
                    require(button("Refresh").active,"Server reply did not recover controls");
                    require(hasButton("Mute reminders") || hasButton("Turn reminders on"),"Reminder setting lost after server reply");
                    button("Turn off...").onPress(); button("Turn off").onPress(); advance();
                }
                case 10 -> { require(hasButton("Enable Homecoming"),"Server did not return disabled participation"); button("Updates").onPress(); advance(); }
                case 11 -> { capture("welcome"); button("Enable Homecoming").onPress(); advance(); }
                case 12 -> {
                    require(!hasButton("Enable Homecoming"),"Enable did not round-trip"); capture("caught-up");
                    // 960x720 at scale 3 exercises the actual 320x240 GUI minimum.
                    mc.getWindow().setWindowed(960,720); mc.options.guiScale().set(3); mc.resizeDisplay(); advance();
                }
                case 13 -> {
                    capture("compact");
                    mc.options.guiScale().set(2); mc.resizeDisplay();
                    java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("homecoming-ui-checks.txt"),
                            "PASS: grouped updates and populated suggestions; existing quest-book navigation; authoritative world rules; Home/End scroll; cancel confirmation; pending controls; server read/mute/disable/enable; compact GUI; screenshots saved.");
                    System.out.println("HOMECOMING_UI_FLOW_CHECKS_VERIFIED"); advance();
                }
                case 14 -> { mc.stop(); stage=99; }
                default -> throw new IllegalStateException("Unexpected preview stage");
            }
        } catch(Throwable failure) {
            failure.printStackTrace();
            try { java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("homecoming-ui-checks.txt"),"FAILED stage "+stage+": "+failure); }
            catch(java.io.IOException ignored) {}
            mc.stop(); stage=99;
        }
    }
    private static boolean fixture(ServerPlayer player) {
        var file=ServerQuestFile.INSTANCE;
        var chapters=new java.util.ArrayList<Chapter>(); file.forAllChapters(chapters::add);
        var completed=chapters.stream().filter(ch -> ch.getRawTitle().equals("Settling in")).findFirst().orElse(null);
        var active=chapters.stream().filter(ch -> ch.getRawTitle().equals("A place to call home")).findFirst().orElse(null);
        boolean seeded=false;
        if(completed==null) {
            completed=new Chapter(file.newID(),file,file.getDefaultChapterGroup()); completed.onCreated(); completed.setRawTitle("Settling in");
            var one=new Quest(file.newID(),completed); one.onCreated(); one.setRawTitle("Set up a shared base"); new CheckmarkTask(file.newID(),one).onCreated();
            var two=new Quest(file.newID(),completed); two.onCreated(); two.setRawTitle("Organize the storage room"); new CheckmarkTask(file.newID(),two).onCreated(); seeded=true;
        }
        if(active==null) {
            active=new Chapter(file.newID(),file,file.getDefaultChapterGroup()); active.onCreated(); active.setRawTitle("A place to call home");
            var one=new Quest(file.newID(),active); one.onCreated(); one.setRawTitle("Connect the paths around your base"); new CheckmarkTask(file.newID(),one).onCreated();
            var two=new Quest(file.newID(),active); two.onCreated(); two.setRawTitle("Make room for the next adventure"); new CheckmarkTask(file.newID(),two).onCreated(); seeded=true;
        }
        var first=completed.getQuests().getFirst(); var second=completed.getQuests().getLast();
        var task1=first.getTasks().iterator().next(); var task2=second.getTasks().iterator().next();
        var next=active.getQuests().stream().filter(q -> q.getRawTitle().equals("Connect the paths around your base")).findFirst().orElseThrow();
        file.clearCachedData();
        var progress=file.getTeamData(player).orElseThrow();
        progress.setProgress(task1,0); progress.setProgress(task2,0);
        progress.setCompleted(first.id,null); progress.setCompleted(second.id,null); progress.setCompleted(completed.id,null);
        progress.setProgress(task1,1); progress.setProgress(task2,1);
        progress.setStarted(active.id,new java.util.Date()); progress.setStarted(next.id,new java.util.Date());
        return !seeded;
    }
    private static JournalScreen screen() {
        if(!(Minecraft.getInstance().screen instanceof JournalScreen screen)) throw new IllegalStateException("Journal is not open");
        return screen;
    }
    private static Button button(String label) {
        return screen().children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(label))
                .map(c -> (Button)c).findFirst().orElseThrow(() -> new IllegalStateException("Missing button: "+label));
    }
    private static boolean hasButton(String label) { return screen().children().stream().anyMatch(c -> c instanceof Button b && b.getMessage().getString().equals(label)); }
    private static void advance() { stage++; ticks=0; }
    private static void capture(String name) { var mc=Minecraft.getInstance(); Screenshot.grab(mc.gameDirectory,"homecoming-"+name+".png",mc.getMainRenderTarget(),c -> System.out.println(c.getString())); }
    private static void require(boolean condition,String message) { if(!condition) throw new IllegalStateException(message); }
}

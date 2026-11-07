package dev.homecoming.client;

import dev.homecoming.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Measured entries share their layout with the viewport's native input widgets. */
public final class JournalScreen extends Screen {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
    private static final String[] TABS={"Updates","Next steps","Settings"};
    private static final JournalIcon[] TAB_ICONS={JournalIcon.BOOK,JournalIcon.COMPASS,JournalIcon.GEAR};
    private BriefingView view;
    private int tab,left,top,panelWidth,panelHeight,bodyX,bodyY,bodyWidth,bodyHeight,scroll,contentHeight,cursor;
    private boolean confirmDisable;
    private String feedback="",pending="";
    private long requestedAt;
    private final List<EntryLayout> entries=new ArrayList<>();
    private final List<PlacedButton> bodyButtons=new ArrayList<>();
    private final List<JournalButton> networkButtons=new ArrayList<>();
    private record PlacedButton(JournalButton button,int y) {}
    private record EntryLayout(int y,int height,JournalIcon icon,String caption,List<FormattedCharSequence> title,List<FormattedCharSequence> details) {}
    private record Action(String label,JournalIcon icon,JournalButton.Kind kind,Runnable run,boolean network) {}
    public JournalScreen(BriefingView view,int tab) { super(Component.literal("Homecoming")); this.view=view; this.tab=tab; }
    public int tab() { return tab; }
    private Action action(String label,JournalIcon icon,Runnable run) { return new Action(label,icon,JournalButton.Kind.NORMAL,run,false); }
    private Action serverAction(String label,JournalIcon icon,String action,long id) { return new Action(label,icon,JournalButton.Kind.PRIMARY,() -> request(action,id),true); }

    @Override protected void init() {
        panelWidth=Math.min(600,width-24); panelHeight=Math.min(390,height-24);
        left=(width-panelWidth)/2; top=(height-panelHeight)/2;
        bodyX=left+12; bodyY=top+76; bodyWidth=panelWidth-28; bodyHeight=Math.max(24,panelHeight-138);
        entries.clear(); bodyButtons.clear(); networkButtons.clear(); cursor=0;
        int tabWidth=(panelWidth-32)/3;
        for(int i=0;i<3;i++) {
            final int next=i;
            addRenderableWidget(new JournalButton(left+12+i*(tabWidth+4),top+48,tabWidth,TABS[i],TAB_ICONS[i],JournalButton.Kind.TAB,tab==i,b -> {
                tab=next; confirmDisable=false; scroll=0; feedback=""; rebuildWidgets();
            }));
        }
        int footer=top+panelHeight-30, x=left+12;
        if(tab==0 && view.enabled() && !view.updates().isEmpty()) {
            var read=new JournalButton(x,footer,126,"Mark these read",JournalIcon.CHECK,JournalButton.Kind.PRIMARY,false,
                    b -> request("ack",view.updates().getLast().id())).hint("Only displayed updates will be marked read. Closing keeps them unread.");
            addRenderableWidget(read); networkButtons.add(read); x+=132;
        }
        var refresh=new JournalButton(x,footer,78,"Refresh",JournalIcon.REFRESH,JournalButton.Kind.NORMAL,false,b -> request("open",0));
        addRenderableWidget(refresh); networkButtons.add(refresh);
        addRenderableWidget(new JournalButton(left+panelWidth-74,footer,62,"Done",JournalIcon.CLOSE,JournalButton.Kind.NORMAL,false,b -> onClose())
                .hint("Close the journal. Unread updates stay unread."));
        if(!view.enabled() && tab!=2) buildWelcome();
        else if(tab==0) buildUpdates(); else if(tab==1) buildSuggestions(); else buildSettings();
        contentHeight=Math.max(0,cursor-8); positionButtons();
    }
    private void buildWelcome() {
        card(JournalIcon.BOOK,"YOUR WORLD, SINCE LAST TIME","A little help coming home",List.of(
                "See your team's milestones and shared places. Find an existing quest to pick up when you're ready.",
                "Enable to record future progress. Opening this journal never marks updates read."),serverAction("Enable Homecoming",JournalIcon.CHECK,"enable",0));
    }
    private void buildUpdates() {
        if(view.historyGap()) card(JournalIcon.WARNING,"HISTORY LIMIT","Some older updates have expired",List.of("Your world keeps a limited history. The entries below are still available."));
        if(view.updates().isEmpty()) card(JournalIcon.CHECK,"UP TO DATE","You're caught up",List.of(
                "New team milestones and shared places will appear here. Head back into the world whenever you're ready."),
                action("Find a next step",JournalIcon.COMPASS,() -> { tab=1; scroll=0; rebuildWidgets(); }));
        for(var update:view.updates()) {
            String caption=(update.place()!=null?"SHARED PLACE":update.count()>1?update.count()+" RELATED UPDATES":"TEAM MILESTONE")+"  /  "+DATE.format(Instant.ofEpochMilli(update.time()));
            if(update.place()!=null) {
                var place=update.place();
                card(JournalIcon.PIN,caption,localized(update.messageJson(),update.message()),List.of(
                        Component.literal(dimensionName(place.dimension())+"  /  "+place.x()+", "+place.y()+", "+place.z())),
                        action(ModList.get().isLoaded("xaerominimap")?"Track in Xaero":"Copy location",JournalIcon.COMPASS,() -> track(update)),
                        action("Copy",JournalIcon.COPY,() -> { copy(place); feedback="Copied dimension and coordinates."; }));
            } else card(JournalIcon.BOOK,caption,localized(update.messageJson(),update.message()),List.of());
        }
    }
    private void buildSuggestions() {
        if(view.suggestions().isEmpty()) {
            card(JournalIcon.COMPASS,"PLAY AT YOUR OWN PACE",view.teamQuests()?"No suggested quests right now":"Your world, your next move",List.of(
                    view.teamQuests()?"There are no available suggestions for your current progress. Your quest book still has the full picture.":
                            "Connect FTB Quests and FTB Teams on the server to see existing quests here. Shared places and vanilla milestones still work.")); return;
        }
        for(var suggestion:view.suggestions()) card(JournalIcon.COMPASS,localized(suggestion.chapterJson(),suggestion.chapter()).getString(),
                localized(suggestion.titleJson(),suggestion.title()),List.of(Component.literal("Available in your existing quest book. Take it on whenever you like.")),
                action("Open quest",JournalIcon.ARROW,() -> openQuest(suggestion)));
    }
    private void buildSettings() {
        if(confirmDisable) {
            card(JournalIcon.WARNING,"PARTICIPATION","Turn off Homecoming?",List.of(
                    "Future updates will stop recording for you, and your access to saved entries will be removed.",
                    "Your teammates keep their shared history. Already seen or copied coordinates cannot be taken back."),
                    action("Keep enabled",JournalIcon.CHECK,() -> { confirmDisable=false; scroll=0; rebuildWidgets(); }),
                    new Action("Turn off",JournalIcon.LEAVE,JournalButton.Kind.DANGER,() -> request("disable",0),true)); return;
        }
        card(JournalIcon.BELL,"PERSONAL PREFERENCE","Return reminders: "+(view.quiet()?"muted":"on"),List.of(
                "A gentle invitation to catch up when you return. You can always open /recap yourself."),
                serverAction(view.quiet()?"Turn reminders on":"Mute reminders",JournalIcon.BELL,"quiet",0));
        if(view.enabled()) card(JournalIcon.CHECK,"PARTICIPATION","Homecoming is on",List.of(
                "Records shared milestones and places for you. No chat logging or chest scanning."),
                action("Turn off...",JournalIcon.LEAVE,() -> { confirmDisable=true; scroll=0; rebuildWidgets(); }));
        else card(JournalIcon.BOOK,"PARTICIPATION","Homecoming is off",List.of("Enable to start recording future shared progress."),
                serverAction("Enable Homecoming",JournalIcon.CHECK,"enable",0));
        boolean clientQuests=ModList.get().isLoaded("ftbquests");
        card(JournalIcon.COMPASS,"CONNECTED TOOLS","Your existing tools",List.of(
                view.teamQuests()?(clientQuests?"Quest book: connected to FTB Quests and your team.":"Team quests are available. Install FTB Quests on this client to open them."):
                        "Quest book: no team quest integration on the server.",
                ModList.get().isLoaded("xaerominimap")?"Map: Xaero tracking is available on shared places.":"Map: coordinates can be copied. Xaero is optional."));
        var rules=view.rules();
        if(rules!=null) card(JournalIcon.LOCK,"READ-ONLY / WORLD OWNER","Rules for this world",List.of(
                "History: "+rules.retentionDays()+" days, up to "+rules.maxEntries()+" entries.",
                "Return reminder: after "+rules.returnHours()+" hours away.","Briefing: up to "+rules.pageSize()+" rows at a time.",
                rules.groupingMinutes()==0?"Related updates: grouping is off.":"Related updates: grouped within "+rules.groupingMinutes()+" minutes."));
    }
    private void card(JournalIcon icon,String caption,String title,List<String> details,Action... actions) {
        card(icon,caption,Component.literal(title),details.stream().map(Component::literal).map(c -> (Component)c).toList(),actions);
    }
    private void card(JournalIcon icon,String caption,Component title,List<Component> details,Action... actions) {
        int textWidth=Math.max(60,bodyWidth-56);
        // Preserve translated wording, but normalize pack-authored colors/font weight
        // to readable ink on the journal's paper. The source quest book keeps its styling.
        var titleLines=font.split(Component.literal(title.getString()),textWidth);
        var detailLines=new ArrayList<FormattedCharSequence>();
        for(var detail:details) detailLines.addAll(font.split(detail,textWidth));
        int y=24+titleLines.size()*11;
        if(!detailLines.isEmpty()) y+=4+detailLines.size()*11;
        if(actions.length>0) {
            y+=8; int buttonX=36,rowY=y;
            for(var action:actions) {
                int buttonWidth=Math.min(bodyWidth-48,font.width(action.label())+32);
                if(buttonX+buttonWidth>bodyWidth-12) { buttonX=36; rowY+=24; }
                var button=new JournalButton(bodyX+buttonX,bodyY+cursor+rowY-scroll,buttonWidth,action.label(),action.icon(),action.kind(),false,b -> action.run().run());
                addRenderableWidget(button); bodyButtons.add(new PlacedButton(button,cursor+rowY));
                if(action.network()) networkButtons.add(button); buttonX+=buttonWidth+8;
            }
            y=rowY+20;
        }
        int height=y+12; entries.add(new EntryLayout(cursor,height,icon,caption,titleLines,detailLines)); cursor+=height+8;
    }
    private void positionButtons() {
        scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-bodyHeight)));
        for(var placed:bodyButtons) {
            int y=bodyY+placed.y()-scroll; placed.button().setY(y);
            placed.button().visible=y>=bodyY && y+20<=bodyY+bodyHeight;
        }
        for(var button:networkButtons) button.active=pending.isEmpty();
    }
    private Component localized(String json,String fallback) {
        if(json==null || minecraft.level==null) return Component.literal(fallback);
        try { var text=Component.Serializer.fromJson(json,minecraft.level.registryAccess()); return text==null?Component.literal(fallback):text; }
        catch(RuntimeException ignored) { return Component.literal(fallback); }
    }
    public static String dimensionName(String dimension) {
        return switch(dimension) { case "minecraft:overworld" -> "Overworld"; case "minecraft:the_nether" -> "The Nether"; case "minecraft:the_end" -> "The End";
            default -> dimension.substring(dimension.indexOf(':')+1).replace('_',' '); };
    }
    private void track(BriefingView.Entry entry) {
        if(ModList.get().isLoaded("xaerominimap")) {
            try { feedback=XaeroIntegration.track(entry.id(),entry.message(),entry.place()); }
            catch(RuntimeException | LinkageError error) { copy(entry.place()); feedback="Map unavailable. Dimension and coordinates copied."; }
        } else { copy(entry.place()); feedback="Copied dimension and coordinates."; }
    }
    private void copy(Journal.Place place) { minecraft.keyboardHandler.setClipboard(place.dimension()+" / "+place.x()+" "+place.y()+" "+place.z()); }
    private void openQuest(BriefingView.Suggestion suggestion) {
        if(!ModList.get().isLoaded("ftbquests")) { feedback="Open your quest book: "+suggestion.chapter()+" / "+suggestion.title(); return; }
        try { QuestNavigation.open(suggestion.questId()); }
        catch(RuntimeException | LinkageError error) { feedback="Find in your quest book: "+suggestion.chapter()+" / "+suggestion.title(); }
    }
    private void request(String action,long id) {
        if(!pending.isEmpty()) return;
        pending=action; requestedAt=System.currentTimeMillis(); feedback=action.equals("open")?"Refreshing your briefing...":"Saving your choice...";
        positionButtons(); PacketDistributor.sendToServer(new HomecomingNetwork.Request(action,id));
    }
    public void receive(BriefingView updated) {
        feedback=switch(pending) {
            case "ack" -> "Those updates are marked read.";
            case "quiet" -> updated.quiet()?"Return reminders are muted.":"Return reminders are on.";
            case "enable" -> "Homecoming is on. Future updates will be recorded.";
            case "disable" -> "Homecoming is off. Your saved entry access was removed.";
            case "open" -> "Your briefing is up to date."; default -> ""; };
        if(pending.equals("ack") || pending.equals("disable")) scroll=0;
        view=updated; pending=""; confirmDisable=false; rebuildWidgets();
    }
    @Override public void tick() {
        if(!pending.isEmpty() && System.currentTimeMillis()-requestedAt>8000) {
            pending=""; feedback="No response yet. Try Refresh to check the saved state."; positionButtons();
        }
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(key==256 && confirmDisable) { confirmDisable=false; scroll=0; rebuildWidgets(); return true; }
        if(key>=266 && key<=269) {
            scroll=switch(key) { case 266 -> scroll-Math.max(24,bodyHeight-12); case 267 -> scroll+Math.max(24,bodyHeight-12); case 268 -> 0; default -> contentHeight; };
            positionButtons(); return true;
        }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        g.fill(0,0,width,height,JournalStyle.SHADE);
        g.fill(left-1,top-1,left+panelWidth+1,top+panelHeight+1,JournalStyle.INK);
        g.fill(left,top,left+panelWidth,top+panelHeight,JournalStyle.PAPER);
        g.fill(left+16,top,left+34,top+40,JournalStyle.SPRUCE);
        g.fill(left+16,top+40,left+20,top+43,JournalStyle.SPRUCE); g.fill(left+30,top+40,left+34,top+43,JournalStyle.SPRUCE);
        JournalIcon.BOOK.draw(g,left+20,top+14,JournalStyle.PAGE);
        g.drawString(font,Component.literal("Homecoming").withStyle(ChatFormatting.BOLD),left+44,top+12,JournalStyle.INK,false);
        String player=minecraft.player==null?"traveler":minecraft.player.getGameProfile().getName();
        g.drawString(font,font.plainSubstrByWidth("Welcome back, "+player+".",panelWidth-140),left+44,top+27,JournalStyle.PENCIL,false);
        String count=tab==0 && view.enabled()?view.updates().size()+(view.updates().size()==1?" update":" updates"):TABS[tab];
        g.drawString(font,count,left+panelWidth-12-font.width(count),top+14,JournalStyle.PENCIL,false);
        g.enableScissor(bodyX,bodyY,bodyX+bodyWidth,bodyY+bodyHeight);
        for(var entry:entries) {
            int y=bodyY+entry.y()-scroll;
            if(y+entry.height()<bodyY || y>bodyY+bodyHeight) continue;
            g.fill(bodyX,y,bodyX+bodyWidth,y+entry.height(),JournalStyle.PAGE);
            g.fill(bodyX,y+entry.height()-1,bodyX+bodyWidth,y+entry.height(),JournalStyle.LINE);
            g.fill(bodyX+10,y+12,bodyX+26,y+28,JournalStyle.CONTROL); entry.icon().draw(g,bodyX+13,y+15,JournalStyle.SPRUCE);
            int textX=bodyX+36,textY=y+12;
            g.drawString(font,font.plainSubstrByWidth(entry.caption(),bodyWidth-48),textX,textY,JournalStyle.PENCIL,false); textY+=12;
            for(var line:entry.title()) { g.drawString(font,line,textX,textY,JournalStyle.INK,false); textY+=11; }
            if(!entry.details().isEmpty()) textY+=4;
            for(var line:entry.details()) { g.drawString(font,line,textX,textY,JournalStyle.PENCIL,false); textY+=11; }
        }
        g.disableScissor();
        if(contentHeight>bodyHeight) {
            int barX=left+panelWidth-9; g.fill(barX,bodyY,barX+2,bodyY+bodyHeight,JournalStyle.LINE);
            int thumb=Math.max(12,bodyHeight*bodyHeight/contentHeight), thumbY=bodyY+(bodyHeight-thumb)*scroll/(contentHeight-bodyHeight);
            g.fill(barX,thumbY,barX+2,thumbY+thumb,JournalStyle.SPRUCE);
        }
        String status=feedback.isEmpty()?(confirmDisable?"You can keep Homecoming enabled.":tab==0 && !view.updates().isEmpty()?"Close anytime. Updates stay unread until you mark them read.":contentHeight>bodyHeight?"Scroll or use PgDn to read more.":"Your journal. Your pace."):feedback;
        var lines=font.split(Component.literal(status),panelWidth-24);
        for(int i=0;i<Math.min(2,lines.size());i++) g.drawString(font,lines.get(i),left+12,top+panelHeight-55+i*10,JournalStyle.PENCIL,false);
        positionButtons(); super.render(g,mouseX,mouseY,partialTick);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x<bodyX || x>bodyX+bodyWidth+8 || y<bodyY || y>bodyY+bodyHeight) return super.mouseScrolled(x,y,horizontal,vertical);
        scroll-=(int)(vertical*24); positionButtons(); return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}

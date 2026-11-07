package dev.homecoming;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;
import java.util.Set;
import java.util.UUID;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.data.AbstractTeamBase;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import com.mojang.authlib.GameProfile;

/** Included only in development runs with -PruntimeChecks; never in the published jar. */
@GameTestHolder("homecoming")
@PrefixGameTestTemplate(false)
public final class RuntimeChecks {
    @GameTest(template="empty")
    public static void ftbMembershipPeriods(GameTestHelper helper) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var server=helper.getLevel().getServer();
        var manager=(TeamManagerImpl)FTBTeamsAPI.api().getManager();
        var owner=new GameProfile(UUID.randomUUID(),"runtime-owner");
        var reader=new GameProfile(UUID.randomUUID(),"runtime-member");
        manager.playerLoggedIn(null,owner.getId(),owner.getName());
        manager.playerLoggedIn(null,reader.getId(),reader.getName());
        var j=JournalData.get(server).journal;
        j.player(reader.getId().toString()).enabled=true;
        var party=manager.createParty(owner.getId(),null,"Homecoming membership check",null,null);
        party.join(null,reader);
        String id=reader.getId().toString(), team=party.getId().toString();
        long now=System.currentTimeMillis();
        j.add(now,"runtime","before-leave",team,Set.of(id),"Before leaving",null);
        helper.assertTrue(j.unread(id,team,now).size()==1,"Joined reader receives current party history");
        party.leave(reader.getId());
        helper.assertTrue(j.player(id).currentTeam.equals(id),"FTB leave event switches journal back to personal team");
        party.join(null,reader);
        helper.assertTrue(j.player(id).currentTeam.equals(team),"FTB rejoin event restores current party");
        helper.assertTrue(j.unread(id,team,now).isEmpty(),"Rejoining must not restore history from prior membership");
        j.add(now+1,"runtime","after-rejoin",team,Set.of(id),"After rejoining",null);
        helper.assertTrue(j.unread(id,team,now+2).size()==1,"New membership still receives future progress");
        System.out.println("HOMECOMING_FTB_MEMBERSHIP_PERIODS_VERIFIED");
        helper.succeed();
    }
    @GameTest(template="empty", timeoutTicks=200)
    public static void groupedBriefingAndSuggestions(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        var player=new ServerPlayer(server,helper.getLevel(),new GameProfile(UUID.randomUUID(),"runtime-planner"),ClientInformation.createDefault());
        var manager=(TeamManagerImpl)FTBTeamsAPI.api().getManager();
        manager.playerLoggedIn(null,player.getUUID(),"Homecoming runtime planner");
        var team=manager.getTeamForPlayer(player).orElseThrow();
        var file=ServerQuestFile.INSTANCE;
        var active=new Chapter(file.newID(),file,file.getDefaultChapterGroup()); active.onCreated();
        var started=new Quest(file.newID(),active); started.onCreated();
        new CheckmarkTask(file.newID(),started).onCreated();
        var next=new Quest(file.newID(),active); next.onCreated();
        new CheckmarkTask(file.newID(),next).onCreated();
        var locked=new Quest(file.newID(),active); locked.onCreated();
        new CheckmarkTask(file.newID(),locked).onCreated(); locked.addDependency(next);
        var other=new Chapter(file.newID(),file,file.getDefaultChapterGroup()); other.onCreated();
        var unrelated=new Quest(file.newID(),other); unrelated.onCreated();
        new CheckmarkTask(file.newID(),unrelated).onCreated();
        file.clearCachedData();
        var progress=file.getOrCreateTeamData(team);
        progress.setStarted(active.id,new java.util.Date());
        progress.setStarted(started.id,new java.util.Date());
        // Even a started quest must not be suggested while its dependency is locked.
        progress.setStarted(locked.id,new java.util.Date());
        var suggestions=Homecoming.integration.suggestions(player);
        helper.assertTrue(suggestions.size()==2,"Briefing should offer two available existing quests");
        helper.assertTrue(suggestions.getFirst().questId().equals(Long.toHexString(started.id)),"Started available quest comes first");
        helper.assertTrue(suggestions.getLast().questId().equals(Long.toHexString(next.id)),"Available quest in active chapter comes before unrelated chapters");

        var completedChapter=new Chapter(file.newID(),file,file.getDefaultChapterGroup()); completedChapter.onCreated();
        var first=new Quest(file.newID(),completedChapter); first.onCreated();
        var firstTask=new CheckmarkTask(file.newID(),first); firstTask.onCreated();
        var second=new Quest(file.newID(),completedChapter); second.onCreated();
        var secondTask=new CheckmarkTask(file.newID(),second); secondTask.onCreated();
        file.clearCachedData();
        var data=JournalData.get(server); data.journal.player(player.getStringUUID()).enabled=true;
        data.journal.updateTeam(player.getStringUUID(),team.getId().toString());
        progress.setProgress(firstTask,1); progress.setProgress(secondTask,1);
        helper.runAfterDelay(5, () -> {
            var view=Homecoming.view(player);
            helper.assertTrue(view.updates().size()==1,"Related actual quest and chapter completions should share one briefing row");
            helper.assertTrue(view.updates().getFirst().count()==3,"Grouped row retains both quest events and chapter completion");
            helper.assertTrue(data.journal.player(player.getStringUUID()).offered==view.updates().getFirst().id(),"Acknowledgment cursor covers the displayed group");
            System.out.println("HOMECOMING_GROUPING_AND_SUGGESTIONS_VERIFIED");
            helper.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=200)
    public static void ftbCompletionAndVisibility(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        // Use an offline identity: GameTest's mock login cannot negotiate FTB channels.
        var player=new ServerPlayer(server,helper.getLevel(),new GameProfile(UUID.randomUUID(),"runtime-reader"),ClientInformation.createDefault());
        var manager=(TeamManagerImpl)FTBTeamsAPI.api().getManager();
        manager.playerLoggedIn(null, player.getUUID(), "Homecoming runtime reader");
        var team=manager.getTeamForPlayer(player).orElseThrow();
        String alice=player.getStringUUID(), bob=UUID.randomUUID().toString(), outsider=UUID.randomUUID().toString();
        ((AbstractTeamBase)team).addMember(UUID.fromString(bob), TeamRank.MEMBER);
        var j=JournalData.get(server).journal;
        for (String id : Set.of(alice,bob,outsider)) j.player(id).enabled=true;
        j.updateTeam(alice,team.getId().toString());
        var file=ServerQuestFile.INSTANCE;
        var chapter=new Chapter(file.newID(),file,file.getDefaultChapterGroup()); chapter.onCreated();
        var quest=new Quest(file.newID(),chapter); quest.onCreated();
        var task=new CheckmarkTask(file.newID(),quest); task.onCreated();
        var hiddenChapter=new Chapter(file.newID(),file,file.getDefaultChapterGroup());
        var nbt=new CompoundTag(); nbt.putBoolean("always_invisible",true);
        hiddenChapter.readData(nbt,server.registryAccess()); hiddenChapter.onCreated();
        var hiddenQuest=new Quest(file.newID(),hiddenChapter); hiddenQuest.onCreated();
        var hiddenTask=new CheckmarkTask(file.newID(),hiddenQuest); hiddenTask.onCreated();
        file.clearCachedData();
        var progress=file.getOrCreateTeamData(team);
        progress.setProgress(task,1); progress.setProgress(hiddenTask,1);
        helper.runAfterDelay(5, () -> {
            var visible=j.unread(alice,team.getId().toString(),System.currentTimeMillis());
            helper.assertTrue(visible.stream().anyMatch(f -> f.key().equals(Long.toHexString(quest.id))), "Real FTB task completion must reach the journal");
            helper.assertTrue(visible.stream().noneMatch(f -> f.key().equals(Long.toHexString(hiddenQuest.id))), "Hidden quest must not be captured");
            helper.assertTrue(j.unread(bob,team.getId().toString(),System.currentTimeMillis()).stream().anyMatch(f -> f.key().equals(Long.toHexString(quest.id))), "Offline teammate is in captured audience");
            helper.assertTrue(j.unread(outsider,team.getId().toString(),System.currentTimeMillis()).isEmpty(), "Nonmember has no access");
            var fact=visible.stream().filter(f -> f.key().equals(Long.toHexString(quest.id))).findFirst().orElseThrow();
            helper.assertTrue(Homecoming.integration.visible(player,fact), "Completed visible source is readable");
            progress.setCompleted(quest.id,null);
            helper.assertFalse(Homecoming.integration.visible(player,fact), "Reset source must disappear from recap");
            System.out.println("HOMECOMING_FTB_EVENT_AND_VISIBILITY_VERIFIED");
            helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void persistedJournal(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("ftbquests"), "FTB Quests must actually load");
        helper.assertTrue(ModList.get().isLoaded("ftbteams"), "FTB Teams must actually load");
        var data = JournalData.get(helper.getLevel().getServer());
        var j = data.journal;
        long now = System.currentTimeMillis();
        String alice="runtime-alice", bob="runtime-bob", team="runtime-team";
        if (j.players.containsKey(alice)) {
            helper.assertTrue(j.unread(alice, team, now).isEmpty(), "Acknowledgment survives disk reload");
            helper.assertTrue(j.unread(bob, team, now).stream().anyMatch(f -> f.key().equals("restart-fixture")), "Offline teammate retains unread history after restart");
            helper.assertTrue(j.unread(bob, "outsiders", now).isEmpty(), "Other teams cannot read persisted facts");
            System.out.println("HOMECOMING_RESTART_VERIFIED");
        } else {
            j.player(alice).enabled=true; j.player(bob).enabled=true;
            j.updateTeam(alice, team); j.updateTeam(bob, team);
            j.add(now, "runtime", "restart-fixture", team, Set.of(alice,bob), "Persistent team update", new Journal.Place("minecraft:the_nether",12,70,-20));
            j.player(alice).offered=j.sequence; j.acknowledge(alice);
            data.setDirty(); helper.getLevel().getServer().overworld().getDataStorage().save();
            System.out.println("HOMECOMING_RESTART_FIXTURE_WRITTEN");
        }
        helper.succeed();
    }
}

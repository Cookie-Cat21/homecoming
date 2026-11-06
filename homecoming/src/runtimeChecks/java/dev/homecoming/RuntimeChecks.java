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

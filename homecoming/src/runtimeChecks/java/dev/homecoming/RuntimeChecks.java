package dev.homecoming;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;
import java.util.Set;

/** Included only in development runs with -PruntimeChecks; never in the published jar. */
@GameTestHolder("homecoming")
@PrefixGameTestTemplate(false)
public final class RuntimeChecks {
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

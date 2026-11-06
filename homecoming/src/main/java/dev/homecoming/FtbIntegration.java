package dev.homecoming;

import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.*;
import java.util.stream.Collectors;

/** Isolated class: never loaded when the optional FTB mods are absent. */
final class FtbIntegration implements Homecoming.Integration {
    FtbIntegration() {
        ObjectCompletedEvent.QUEST.register(e -> {
            record(e.getData(), e.getQuest(), "ftb.quest");
            return EventResult.pass();
        });
        ObjectCompletedEvent.CHAPTER.register(e -> {
            record(e.getData(), e.getChapter(), "ftb.chapter");
            return EventResult.pass();
        });
    }
    private Optional<Team> teamObject(ServerPlayer player) {
        return FTBTeamsAPI.api().isManagerLoaded()
                ? FTBTeamsAPI.api().getManager().getTeamForPlayer(player) : Optional.empty();
    }
    @Override public String team(ServerPlayer player) {
        return teamObject(player).map(t -> t.getId().toString()).orElse(player.getStringUUID());
    }
    @Override public Set<String> members(ServerPlayer player) {
        return teamObject(player).map(t -> t.getMembers().stream().map(UUID::toString).collect(Collectors.toSet()))
                .orElse(Set.of(player.getStringUUID()));
    }
    private void record(TeamData progress, QuestObject object, String source) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || object.getQuestFile() != ServerQuestFile.INSTANCE || !FTBTeamsAPI.api().isManagerLoaded()) return;
        var audience = FTBTeamsAPI.api().getManager().getTeamByID(progress.getTeamId());
        if (audience.isEmpty()) return;
        var recipients = audience.get().getMembers().stream().map(UUID::toString).collect(Collectors.toSet());
        var teamId = progress.getTeamId().toString();
        // Completion callbacks can precede the source state settling; validate on the server task queue.
        server.execute(() -> {
            if (!progress.isCompleted(object) || !object.isVisible(progress)) return;
            var data = JournalData.get(server);
            data.journal.add(System.currentTimeMillis(), source, Long.toHexString(object.id), teamId, recipients,
                    "Your team completed " + (object instanceof Chapter ? "chapter: " : "quest: ") + object.getTitle().getString(), null);
            data.setDirty();
        });
    }
    @Override public boolean visible(ServerPlayer player, Journal.Fact fact) {
        if (!fact.source().startsWith("ftb.")) return true;
        var file = ServerQuestFile.INSTANCE;
        if (file == null) return false;
        var progress = file.getTeamData(player);
        if (progress.isEmpty()) return false;
        var object = file.get(Long.parseUnsignedLong(fact.key(), 16));
        return object != null && object.isVisible(progress.get()) && progress.get().isCompleted(object);
    }
    @Override public List<String> suggestions(ServerPlayer player) {
        var file = ServerQuestFile.INSTANCE;
        if (file == null) return List.of();
        var progress = file.getTeamData(player);
        if (progress.isEmpty()) return List.of();
        var result = new ArrayList<String>();
        file.forAllQuests(q -> {
            if (result.size() < 2 && q.isVisible(progress.get()) && q.getChapter().isVisible(progress.get())
                    && !progress.get().isCompleted(q) && progress.get().canStartTasks(q))
                result.add(q.getChapter().getTitle().getString() + " / " + q.getTitle().getString());
        });
        return result;
    }
    @Override public String status() { return "FTB Quests + FTB Teams active; shared completion, available quests and team locations"; }
}

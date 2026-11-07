package dev.homecoming;

import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.*;
import java.util.stream.Collectors;

/** Isolated class: never loaded when the optional FTB mods are absent. */
final class FtbIntegration implements Homecoming.Integration {
    FtbIntegration() {
        TeamEvent.PLAYER_CHANGED.register(event -> {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            var data = JournalData.get(server);
            data.journal.updateTeam(event.getPlayerId().toString(), event.getTeam().getId().toString());
            data.setDirty();
        });
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
            if (!progress.isCompleted(object) || !sourceVisible(object, progress)) return;
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
        return object != null && sourceVisible(object, progress.get()) && progress.get().isCompleted(object);
    }
    private static boolean sourceVisible(QuestObject object, TeamData progress) {
        return object.isVisible(progress) && (!(object instanceof Quest quest) || quest.getChapter().isVisible(progress));
    }
    @Override public List<BriefingView.Suggestion> suggestions(ServerPlayer player) {
        var file = ServerQuestFile.INSTANCE;
        if (file == null) return List.of();
        var progress = file.getTeamData(player);
        if (progress.isEmpty()) return List.of();
        var candidates = new ArrayList<Quest>();
        file.forAllQuests(q -> {
            if (q.isVisible(progress.get()) && q.getChapter().isVisible(progress.get())
                    && !progress.get().isCompleted(q) && progress.get().canStartTasks(q))
                candidates.add(q);
        });
        candidates.sort(Comparator.<Quest>comparingInt(q -> progress.get().isStarted(q) ? 0 : 1)
                .thenComparingInt(q -> progress.get().isStarted(q.getChapter()) ? 0 : 1).thenComparingLong(q -> q.id));
        return candidates.stream().limit(2).map(q -> new BriefingView.Suggestion(q.getChapter().getTitle().getString(),
                q.getTitle().getString(), Long.toHexString(q.id),
                Component.Serializer.toJson(q.getChapter().getTitle(), player.registryAccess()),
                Component.Serializer.toJson(q.getTitle(), player.registryAccess()))).toList();
    }
    @Override public String chapterKey(Journal.Fact fact) {
        if (!fact.source().startsWith("ftb.") || ServerQuestFile.INSTANCE == null) return "";
        var object = ServerQuestFile.INSTANCE.get(Long.parseUnsignedLong(fact.key(), 16));
        if (object instanceof Quest q) return Long.toHexString(q.getChapter().id);
        return object instanceof Chapter chapter ? Long.toHexString(chapter.id) : "";
    }
    @Override public Component rowMessage(Briefing.Row row) {
        if (ServerQuestFile.INSTANCE == null) return Homecoming.Integration.super.rowMessage(row);
        QuestObject object;
        try { object = ServerQuestFile.INSTANCE.get(Long.parseUnsignedLong(row.sourceKey(), 16)); }
        catch (NumberFormatException ignored) { return Homecoming.Integration.super.rowMessage(row); }
        if (object instanceof Chapter chapter)
            return Component.literal("Your team completed chapter: ").append(chapter.getTitle());
        if (object instanceof Quest quest) return row.count() > 1
                ? Component.literal("Your team completed " + row.count() + " updates in ").append(quest.getChapter().getTitle())
                : Component.literal("Your team completed quest: ").append(quest.getTitle());
        return Homecoming.Integration.super.rowMessage(row);
    }
    @Override public String status() { return "FTB Quests + FTB Teams active; shared completion, available quests and team locations"; }
    @Override public boolean hasTeamQuests() { return true; }
}

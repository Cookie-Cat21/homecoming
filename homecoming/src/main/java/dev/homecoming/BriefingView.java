package dev.homecoming;

import java.util.List;

/** Wire DTO: the server builds an already authorized view; no client can request arbitrary team data. */
public record BriefingView(boolean enabled, boolean quiet, boolean historyGap, List<Entry> updates,
                           List<Suggestion> suggestions, String status, WorldRules rules, boolean teamQuests) {
    public record WorldRules(int returnHours, int retentionDays, int maxEntries, int pageSize, int groupingMinutes) {}
    public record Entry(long id, long time, String message, String messageJson, Journal.Place place, int count) {}
    public record Suggestion(String chapter, String title, String questId, String chapterJson, String titleJson) {}
}

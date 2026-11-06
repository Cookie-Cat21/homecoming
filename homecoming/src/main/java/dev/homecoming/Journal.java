package dev.homecoming;

import java.util.*;

/** Server-thread-confined facts require event-time access and current team membership. */
public final class Journal {
    public static final long RETENTION_MS = 30L * 24 * 60 * 60 * 1000;
    public static final int MAX_FACTS = 2000;
    public long sequence;
    public long droppedThrough;
    public List<Fact> facts = new ArrayList<>();
    public Map<String, PlayerState> players = new HashMap<>();
    private transient long retentionMs = RETENTION_MS;
    private transient int maxFacts = MAX_FACTS;
    public static final class PlayerState {
        public boolean enabled;
        public long acknowledged;
        public long offered;
        public long lastLogout;
        public String currentTeam;
        public long membershipStart;
        public boolean quiet;
    }
    public record Place(String dimension, int x, int y, int z) {}
    public void configure(long retentionMs, int maxFacts) {
        if (retentionMs <= 0 || maxFacts < 1) throw new IllegalArgumentException("Invalid history limits");
        this.retentionMs = retentionMs;
        this.maxFacts = maxFacts;
    }
    public void updateTeam(String player, String team) {
        var state = player(player);
        if (state.currentTeam != null && !state.currentTeam.equals(team)) {
            state.membershipStart = sequence;
            state.offered = Math.max(state.offered, sequence);
        }
        state.currentTeam = team;
    }
    public record Fact(long id, long time, String source, String key, String team,
                       Set<String> recipients, String message, Place place) {}
    public PlayerState player(String id) {
        return players.computeIfAbsent(id, ignored -> {
            var state = new PlayerState();
            state.acknowledged = sequence;
            state.offered = sequence;
            return state;
        });
    }
    public void add(long now, String source, String key, String team, Set<String> recipients,
                    String message, Place place) {
        prune(now);
        if (facts.stream().anyMatch(f -> f.source.equals(source) && f.key.equals(key)
                && f.team.equals(team) && now - f.time < 60_000)) return;
        var enrolled = new HashSet<String>();
        for (String id : recipients) {
            var state = players.get(id);
            if (state != null && state.enabled) enrolled.add(id);
        }
        if (enrolled.isEmpty()) return;
        facts.add(new Fact(++sequence, now, source, key, team, Set.copyOf(enrolled), message, place));
        prune(now);
    }
    public List<Fact> unread(String player, String currentTeam, long now) {
        prune(now);
        var state = player(player);
        if (!state.enabled) return List.of();
        return facts.stream().filter(f -> f.id > Math.max(state.acknowledged, state.membershipStart)
                && f.recipients.contains(player) && f.team.equals(currentTeam)).toList();
    }
    public void acknowledge(String player) {
        var state = player(player);
        state.acknowledged = Math.max(state.acknowledged, state.offered);
    }
    public void disable(String player) {
        var state = player(player);
        state.enabled = false;
        state.acknowledged = sequence;
        state.offered = sequence;
        facts.replaceAll(f -> {
            var recipients = new HashSet<>(f.recipients);
            recipients.remove(player);
            return new Fact(f.id, f.time, f.source, f.key, f.team, Set.copyOf(recipients), f.message, f.place);
        });
        facts.removeIf(f -> f.recipients.isEmpty());
    }
    public void prune(long now) {
        while (!facts.isEmpty() && (facts.size() > maxFacts || now - facts.getFirst().time > retentionMs)) {
            droppedThrough = Math.max(droppedThrough, facts.removeFirst().id);
        }
    }
}

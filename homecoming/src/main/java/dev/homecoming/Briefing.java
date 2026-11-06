package dev.homecoming;

import java.util.*;
import java.util.function.Function;

/** Groups only a consecutive prefix, so acknowledging a page cannot eat unseen updates. */
public final class Briefing {
    public record Row(long through, long time, String message, Journal.Place place, int count, String sourceKey) {}
    public static List<Row> select(List<Journal.Fact> facts, int pageSize, long groupWindow,
                                   Function<Journal.Fact, String> chapterKey) {
        var rows = new ArrayList<Row>();
        String previousGroup = "";
        for (var fact : facts) {
            String group = chapterKey.apply(fact);
            Row previous = rows.isEmpty() ? null : rows.getLast();
            boolean merge = groupWindow > 0 && previous != null && !group.isEmpty() && group.equals(previousGroup)
                    && fact.time() - previous.time() <= groupWindow;
            if (!merge && rows.size() >= pageSize) break;
            if (merge) {
                int count = previous.count() + 1;
                String message = fact.source().equals("ftb.chapter") ? fact.message()
                        : "Your team completed " + count + " related quest updates";
                rows.set(rows.size() - 1, new Row(fact.id(), fact.time(), message, null, count, fact.key()));
            } else rows.add(new Row(fact.id(), fact.time(), fact.message(), fact.place(), 1, fact.key()));
            previousGroup = group;
        }
        return rows;
    }
    private Briefing() {}
}

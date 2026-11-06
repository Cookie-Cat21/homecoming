package dev.homecoming;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BriefingTest {
    private Journal.Fact fact(long id, long time, String chapter) {
        return new Journal.Fact(id, time, "ftb.quest", chapter, "team", Set.of("alice"), "Quest " + id, null);
    }
    @Test void groupsRelatedPrefixWithoutConsumingTheNextPage() {
        var facts = List.of(fact(1, 100, "a"), fact(2, 110, "a"), fact(3, 120, "b"), fact(4, 130, "a"));
        var rows = Briefing.select(facts, 1, 1000, Journal.Fact::key);
        assertEquals(1, rows.size());
        assertEquals(2, rows.getFirst().through());
        assertEquals(2, rows.getFirst().count());
        // The remaining nonconsecutive update must be shown on a later page.
        assertEquals(2, Briefing.select(facts.subList(2, 4), 2, 1000, Journal.Fact::key).size());
    }
    @Test void zeroWindowDisablesGroupingEvenAtIdenticalTimestamps() {
        assertEquals(2, Briefing.select(List.of(fact(1, 100, "a"), fact(2, 100, "a")), 5, 0, Journal.Fact::key).size());
    }
    @Test void unrelatedOrOldUpdatesStaySeparate() {
        assertEquals(2, Briefing.select(List.of(fact(1, 100, "a"), fact(2, 2000, "a")), 5, 1000, Journal.Fact::key).size());
        assertEquals(2, Briefing.select(List.of(fact(1, 100, ""), fact(2, 101, "")), 5, 1000, Journal.Fact::key).size());
    }
}

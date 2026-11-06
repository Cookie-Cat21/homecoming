package dev.homecoming;

import org.junit.jupiter.api.Test;
import java.util.Set;
import com.google.gson.Gson;
import static org.junit.jupiter.api.Assertions.*;

class JournalTest {
    private Journal enrolled() {
        var journal = new Journal();
        journal.player("alice").enabled = true;
        journal.player("bob").enabled = true;
        return journal;
    }
    @Test void requiresBothHistoricalAudienceAndCurrentTeam() {
        var j = enrolled();
        j.add(100, "quest", "q", "team", Set.of("alice", "bob"), "Team completed Q", null);
        j.player("newcomer").enabled = true;
        assertEquals(1, j.unread("alice", "team", 101).size());
        assertTrue(j.unread("alice", "other-team", 101).isEmpty());
        assertTrue(j.unread("newcomer", "team", 101).isEmpty());
    }
    @Test void viewingDoesNotConsumeAndAcknowledgmentDoesNotLoseNewEvents() {
        var j = enrolled();
        j.add(100, "quest", "q1", "team", Set.of("alice"), "First", null);
        var visible = j.unread("alice", "team", 101);
        j.player("alice").offered = visible.getLast().id();
        assertEquals(1, j.unread("alice", "team", 102).size());
        j.add(103, "quest", "q2", "team", Set.of("alice"), "Second", null);
        j.acknowledge("alice");
        assertEquals("Second", j.unread("alice", "team", 104).getFirst().message());
    }
    @Test void optOutRemovesAccessWithoutDeletingOtherPlayersHistory() {
        var j = enrolled();
        j.add(100, "quest", "q", "team", Set.of("alice", "bob"), "First", null);
        j.disable("alice");
        j.player("alice").enabled = true;
        assertTrue(j.unread("alice", "team", 101).isEmpty());
        assertEquals(1, j.unread("bob", "team", 101).size());
    }
    @Test void deduplicatesAndBoundsHistory() {
        var j = enrolled();
        j.add(100, "quest", "q", "team", Set.of("alice"), "First", null);
        j.add(101, "quest", "q", "team", Set.of("alice"), "Duplicate", null);
        assertEquals(1, j.facts.size());
        for (int i = 0; i < 2100; i++)
            j.add(102 + i, "quest", "q" + i, "team", Set.of("alice"), "Next", null);
        assertEquals(Journal.MAX_FACTS, j.facts.size());
        assertTrue(j.droppedThrough > 0);
        j.prune(Journal.RETENTION_MS + 3000);
        assertTrue(j.facts.isEmpty());
    }
    @Test void noEnrollmentMeansNoFactsAndNoImplicitEnrollment() {
        var j = new Journal();
        j.add(100, "quest", "q", "team", Set.of("alice"), "First", null);
        assertTrue(j.facts.isEmpty());
        assertFalse(j.player("alice").enabled);
    }
    @Test void serializedJournalRetainsAudienceLocationsAndUnreadCursor() {
        var j = enrolled();
        var place = new Journal.Place("minecraft:the_nether", -12, 70, 25);
        j.add(100, "place", "storage", "team", Set.of("alice", "bob"), "Shared storage", place);
        j.player("alice").offered = 1;
        j.acknowledge("alice");
        var gson = new Gson();
        var restored = gson.fromJson(gson.toJson(j), Journal.class);
        assertTrue(restored.unread("alice", "team", 101).isEmpty());
        assertEquals(place, restored.unread("bob", "team", 101).getFirst().place());
        assertTrue(restored.unread("bob", "other-team", 101).isEmpty());
    }
}

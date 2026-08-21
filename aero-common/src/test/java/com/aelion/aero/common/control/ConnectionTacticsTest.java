package com.aelion.aero.common.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ConnectionTacticsTest {

    @Test
    void fillFirstPrefersFullerLobby() {
        List<ConnectionTactics.Candidate> members = Arrays.asList(
                candidate("a", 2),
                candidate("b", 5)
        );
        ConnectionTactics.Candidate got = ConnectionTactics.pickMember(
                members, "fill_first", "g1", null, new Random(1));
        assertNotNull(got);
        assertEquals("b", got.name());
    }

    @Test
    void balancePrefersEmptierLobby() {
        List<ConnectionTactics.Candidate> members = Arrays.asList(
                candidate("a", 4),
                candidate("b", 1)
        );
        ConnectionTactics.Candidate got = ConnectionTactics.pickMember(
                members, "balance", "g1", null, new Random(1));
        assertNotNull(got);
        assertEquals("b", got.name());
    }

    @Test
    void roundRobinCyclesByName() {
        ConnectionTactics.RoundRobinState rr = new ConnectionTactics.RoundRobinState();
        List<ConnectionTactics.Candidate> members = Arrays.asList(
                candidate("a", 0),
                candidate("b", 0),
                candidate("c", 0)
        );
        assertEquals("a", ConnectionTactics.pickMember(members, "round_robin", "g1", rr, new Random(1)).name());
        assertEquals("b", ConnectionTactics.pickMember(members, "round_robin", "g1", rr, new Random(1)).name());
        assertEquals("c", ConnectionTactics.pickMember(members, "round_robin", "g1", rr, new Random(1)).name());
        assertEquals("a", ConnectionTactics.pickMember(members, "round_robin", "g1", rr, new Random(1)).name());
    }

    @Test
    void pickInitialUsesGroupStrategy() {
        ConnectionTactics.RoundRobinState rr = new ConnectionTactics.RoundRobinState();
        List<ConnectionTactics.Candidate> candidates = Arrays.asList(
                new ConnectionTactics.Candidate(
                        "lobby-b", "grp", "round_robin", 0, 20, true, true, ProxyBackendRole.LOBBY),
                new ConnectionTactics.Candidate(
                        "lobby-a", "grp", "round_robin", 0, 20, true, true, ProxyBackendRole.LOBBY),
                new ConnectionTactics.Candidate(
                        "game", "other", "balance", 0, 20, true, true, ProxyBackendRole.BACKEND)
        );
        assertEquals("lobby-a", ConnectionTactics.pickInitialServer(candidates, null, rr, new Random(1)));
        assertEquals("lobby-b", ConnectionTactics.pickInitialServer(candidates, null, rr, new Random(1)));
    }

    @Test
    void pickInitialFallsBackWithoutGroupMetadata() {
        List<ConnectionTactics.Candidate> candidates = Arrays.asList(
                new ConnectionTactics.Candidate(
                        "lobby-z", "", "", 0, 20, true, true, ProxyBackendRole.LOBBY),
                new ConnectionTactics.Candidate(
                        "lobby-a", "", "", 0, 20, true, true, ProxyBackendRole.LOBBY)
        );
        assertEquals("lobby-z", ConnectionTactics.pickInitialServer(candidates, null, null, new Random(1)));
    }

    @Test
    void pickInitialReturnsNullWhenEmpty() {
        assertNull(ConnectionTactics.pickInitialServer(Collections.<ConnectionTactics.Candidate>emptyList(), null, null));
    }

    private static ConnectionTactics.Candidate candidate(String name, int players) {
        return new ConnectionTactics.Candidate(
                name, "g1", "balance", players, 20, true, true, ProxyBackendRole.LOBBY);
    }
}

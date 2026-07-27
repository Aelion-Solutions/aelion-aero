package com.aelion.aero.common.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aelion.aero.api.FleetGroupSnapshot;
import com.aelion.aero.api.FleetServerSnapshot;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FleetTransferResolverTest {

    @Test
    void findServerByIdOrName() {
        FleetServerSnapshot a = server("id-a", "Lobby", "lobby", true, 2);
        FleetServerSnapshot b = server("id-b", "Game", "game", true, 5);
        List<FleetServerSnapshot> list = Arrays.asList(a, b);
        assertSame(a, FleetTransferResolver.findServer(list, "id-a"));
        assertSame(b, FleetTransferResolver.findServer(list, "game"));
        assertSame(b, FleetTransferResolver.findServer(list, "GAME"));
        assertNull(FleetTransferResolver.findServer(list, "missing"));
    }

    @Test
    void pickJoinableMemberLowestPlayers() {
        FleetServerSnapshot full = server("1", "A", "a", true, 10);
        FleetServerSnapshot light = server("2", "B", "b", true, 1);
        FleetServerSnapshot closed = server("3", "C", "c", false, 0);
        FleetGroupSnapshot group = new FleetGroupSnapshot(
                "g1", "Group", "ACTIVE", 11, 60, 3, "ok",
                Arrays.asList(full, light, closed));
        assertSame(light, FleetTransferResolver.pickJoinableMember(group));
    }

    @Test
    void pickJoinableMemberNameTieBreak() {
        FleetServerSnapshot zebra = server("z", "Zebra", "z", true, 3);
        FleetServerSnapshot alpha = server("a", "Alpha", "a", true, 3);
        FleetGroupSnapshot group = new FleetGroupSnapshot(
                "g1", "Group", "ACTIVE", 6, 40, 2, "ok",
                Arrays.asList(zebra, alpha));
        assertSame(alpha, FleetTransferResolver.pickJoinableMember(group));
    }

    @Test
    void pickJoinableMemberNone() {
        FleetServerSnapshot closed = server("1", "A", "a", false, 0);
        FleetGroupSnapshot group = new FleetGroupSnapshot(
                "g1", "Group", "ACTIVE", 0, 20, 1, "ok",
                Collections.singletonList(closed));
        assertNull(FleetTransferResolver.pickJoinableMember(group));
    }

    @Test
    void proxyNameFallsBackToName() {
        FleetServerSnapshot withProxy = server("1", "Lobby", "lobby_1", true, 0);
        assertEquals("lobby_1", FleetTransferResolver.proxyNameOf(withProxy));
        FleetServerSnapshot noProxy = new FleetServerSnapshot(
                "1", "Lobby", null, null, null, 0, 20, null, null, true, null);
        assertEquals("Lobby", FleetTransferResolver.proxyNameOf(noProxy));
    }

    private static FleetServerSnapshot server(
            String id, String name, String proxy, boolean joinable, int players) {
        return new FleetServerSnapshot(
                id, name, "RUNNING", "PAPER", "online", players, 20, null, null, joinable, proxy);
    }
}

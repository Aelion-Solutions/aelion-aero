package com.aelion.aero.common.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aelion.aero.common.control.ControlFleetNotifyRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FleetNotifyDiffTest {

    @Test
    void statusChangeRunningToStopped() {
        Map<String, FleetNotifySnapshot> prev = mapOf(
                snap("s1", "lobby-1", "running", null, null));
        Map<String, FleetNotifySnapshot> curr = mapOf(
                snap("s1", "lobby-1", "stopped", null, null));
        List<String> lines = FleetNotifyDiff.diff(prev, curr, null);
        assertEquals(1, lines.size());
        assertEquals("lobby-1 stopped", lines.get(0));
    }

    @Test
    void scaledGroupIncluded() {
        Map<String, FleetNotifySnapshot> prev = mapOf(
                snap("s2", "survival-3", "starting", "g1", "Survival"));
        Map<String, FleetNotifySnapshot> curr = mapOf(
                snap("s2", "survival-3", "running", "g1", "Survival"));
        List<String> lines = FleetNotifyDiff.diff(prev, curr, null);
        assertEquals(1, lines.size());
        assertEquals("survival-3 started · scaled · Survival", lines.get(0));
    }

    @Test
    void appearAndDisappear() {
        Map<String, FleetNotifySnapshot> prev = mapOf(
                snap("old", "old-1", "running", "g1", "BedWars"));
        Map<String, FleetNotifySnapshot> curr = mapOf(
                snap("new", "new-1", "running", null, null));
        List<String> lines = FleetNotifyDiff.diff(prev, curr, null);
        assertEquals(2, lines.size());
        assertTrue(lines.contains("new-1 started"));
        assertTrue(lines.contains("old-1 removed · scaled · BedWars"));
    }

    @Test
    void skipsSelfServerId() {
        Map<String, FleetNotifySnapshot> prev = mapOf(
                snap("self", "me", "running", null, null));
        Map<String, FleetNotifySnapshot> curr = mapOf(
                snap("self", "me", "stopped", null, null));
        List<String> lines = FleetNotifyDiff.diff(prev, curr, "self");
        assertTrue(lines.isEmpty());
    }

    @Test
    void pushFormatsScaledRunning() {
        ControlFleetNotifyRequest.ControlFleetNotifyEvent event =
                new ControlFleetNotifyRequest.ControlFleetNotifyEvent();
        event.setId("s2");
        event.setName("survival-3");
        event.setStatus("running");
        event.setGroupId("g1");
        event.setGroupName("Survival");
        assertEquals("survival-3 started · scaled · Survival", FleetNotifyService.formatEvent(event));
    }

    @Test
    void pushFormatsRemoved() {
        ControlFleetNotifyRequest.ControlFleetNotifyEvent event =
                new ControlFleetNotifyRequest.ControlFleetNotifyEvent();
        event.setName("old-1");
        event.setStatus("removed");
        event.setGroupName("BedWars");
        event.setGroupId("g1");
        assertEquals("old-1 removed · scaled · BedWars", FleetNotifyService.formatEvent(event));
    }

    @Test
    void pushIgnoresUnknownStatus() {
        ControlFleetNotifyRequest.ControlFleetNotifyEvent event =
                new ControlFleetNotifyRequest.ControlFleetNotifyEvent();
        event.setName("x");
        event.setStatus("weird");
        assertNull(FleetNotifyService.formatEvent(event));
    }

    @Test
    void deliverPushedToSubscribers() {
        AtomicReference<com.aelion.aero.common.config.AeroConfig> cfg =
                new AtomicReference<>(new com.aelion.aero.common.config.AeroConfig(
                        "https://panel.example",
                        "proxy-1",
                        "tok",
                        com.aelion.aero.common.config.AeroConfig.ControlConfig.disabled()));
        List<String> delivered = new ArrayList<>();
        FleetNotifyService service = new FleetNotifyService(cfg::get, (id, line) -> delivered.add(line));
        UUID player = UUID.randomUUID();
        assertTrue(service.setEnabled(player, true));

        ControlFleetNotifyRequest.ControlFleetNotifyEvent event =
                new ControlFleetNotifyRequest.ControlFleetNotifyEvent();
        event.setId("s1");
        event.setName("lobby-1");
        event.setStatus("stopped");
        int n = service.deliverPushed(Collections.singletonList(event));
        assertEquals(1, n);
        assertEquals(1, delivered.size());
        assertTrue(delivered.get(0).contains("lobby-1 stopped"));
    }

    private static FleetNotifySnapshot snap(
            String id, String name, String status, String groupId, String groupName
    ) {
        return new FleetNotifySnapshot(id, name, status, groupId, groupName);
    }

    private static Map<String, FleetNotifySnapshot> mapOf(FleetNotifySnapshot... snaps) {
        Map<String, FleetNotifySnapshot> map = new LinkedHashMap<>();
        for (FleetNotifySnapshot snap : snaps) {
            map.put(snap.id(), snap);
        }
        return map;
    }
}

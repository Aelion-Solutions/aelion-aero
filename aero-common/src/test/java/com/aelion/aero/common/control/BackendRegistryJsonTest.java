package com.aelion.aero.common.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aelion.aero.common.json.AeroJson;
import org.junit.jupiter.api.Test;

class BackendRegistryJsonTest {

    @Test
    void roundTrip() throws Exception {
        BackendRegistry registry = new BackendRegistry();
        registry.getBackends().add(new BackendEntry("lobby", "127.0.0.1:25565", ProxyBackendRole.LOBBY));
        registry.getBackends().add(new BackendEntry("game-1", "10.0.0.2:25566", ProxyBackendRole.BACKEND));

        String json = AeroJson.mapper().writeValueAsString(registry);
        BackendRegistry parsed = AeroJson.mapper().readValue(json, BackendRegistry.class);

        assertEquals(2, parsed.validBackends().size());
        assertEquals("lobby", parsed.validBackends().get(0).getName());
        assertEquals(ProxyBackendRole.LOBBY, parsed.validBackends().get(0).getRole());
        assertTrue(json.contains("\"role\":\"lobby\""));
    }
}

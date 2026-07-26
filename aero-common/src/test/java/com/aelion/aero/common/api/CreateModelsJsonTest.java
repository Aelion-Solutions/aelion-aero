package com.aelion.aero.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aelion.aero.common.json.AeroJson;
import org.junit.jupiter.api.Test;

class CreateModelsJsonTest {

    @Test
    void createServerRoundTrip() throws Exception {
        CreateServerRequest req = new CreateServerRequest();
        req.setName("dev-1");
        req.setType("survival");
        req.setSoftware("paper");
        req.setMemory(2048);

        String json = AeroJson.mapper().writeValueAsString(req);
        CreateServerRequest parsed = AeroJson.mapper().readValue(json, CreateServerRequest.class);

        assertEquals("dev-1", parsed.getName());
        assertEquals("survival", parsed.getType());
        assertEquals(2048, parsed.getMemory());
    }

    @Test
    void createGroupRoundTrip() throws Exception {
        GroupScaling scaling = new GroupScaling();
        scaling.setMinInstances(1);
        scaling.setMaxInstances(5);
        scaling.setCooldownPeriod(60);

        GroupServerConfig serverConfig = new GroupServerConfig();
        serverConfig.setMemory(4096);
        serverConfig.setMaxPlayers(20);
        serverConfig.setProxyRole("lobby");

        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("bedwars");
        req.setType("minigame");
        req.setTemplateId("tmpl_1");
        req.setScaling(scaling);
        req.setServerConfig(serverConfig);

        String json = AeroJson.mapper().writeValueAsString(req);
        CreateGroupRequest parsed = AeroJson.mapper().readValue(json, CreateGroupRequest.class);

        assertEquals("bedwars", parsed.getName());
        assertNotNull(parsed.getScaling());
        assertEquals(5, parsed.getScaling().getMaxInstances());
        assertEquals(4096, parsed.getServerConfig().getMemory());
        assertEquals("lobby", parsed.getServerConfig().getProxyRole());
    }
}

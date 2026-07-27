package com.aelion.aero.common.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aelion.aero.common.json.AeroJson;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ControlPlayersResponseTest {

    @Test
    void emptySerializesPlayersArray() throws Exception {
        String json = AeroJson.mapper().writeValueAsString(ControlPlayersResponse.empty());
        JsonNode node = AeroJson.mapper().readTree(json);
        assertTrue(node.path("players").isArray());
        assertEquals(0, node.path("players").size());
    }

    @Test
    void entriesSerializeUuidAndName() throws Exception {
        ControlPlayersResponse body = ControlPlayersResponse.of(Collections.singletonList(
                new ControlPlayerEntry("11111111-1111-1111-1111-111111111111", "Steve")));
        String json = AeroJson.mapper().writeValueAsString(body);
        JsonNode node = AeroJson.mapper().readTree(json);
        assertEquals(1, node.path("players").size());
        assertEquals("11111111-1111-1111-1111-111111111111", node.path("players").get(0).path("uuid").asText());
        assertEquals("Steve", node.path("players").get(0).path("name").asText());
    }
}

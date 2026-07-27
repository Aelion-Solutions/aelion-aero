package com.aelion.aero.common.control;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.aelion.aero.common.json.AeroJson;
import org.junit.jupiter.api.Test;

class ControlShutdownResponseTest {

    @Test
    void acceptedSerializesOkTrue() throws Exception {
        String json = AeroJson.mapper().writeValueAsString(ControlShutdownResponse.accepted());
        JsonNode node = AeroJson.mapper().readTree(json);
        assertTrue(node.path("ok").asBoolean());
    }
}

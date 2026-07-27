package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body for {@code POST /v1/players/kick}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlKickRequest {

    private String uuid;
    private String message;

    public ControlKickRequest() {
    }

    public ControlKickRequest(String uuid, String message) {
        this.uuid = uuid;
        this.message = message;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

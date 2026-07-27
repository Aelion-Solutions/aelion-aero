package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response for {@code POST /v1/shutdown}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlShutdownResponse {

    private boolean ok = true;

    public ControlShutdownResponse() {
    }

    public static ControlShutdownResponse accepted() {
        return new ControlShutdownResponse();
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}

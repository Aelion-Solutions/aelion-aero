package com.aelion.aero.common.control;

/**
 * Success body for player control actions.
 */
public final class ControlPlayerActionResponse {

    private boolean ok;

    public ControlPlayerActionResponse() {
    }

    public ControlPlayerActionResponse(boolean ok) {
        this.ok = ok;
    }

    public static ControlPlayerActionResponse ok() {
        return new ControlPlayerActionResponse(true);
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}

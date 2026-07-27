package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body for successful {@code POST /v1/fleet-notify}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlFleetNotifyResponse {

    private boolean ok;
    private int delivered;

    public ControlFleetNotifyResponse() {
    }

    public static ControlFleetNotifyResponse ok(int delivered) {
        ControlFleetNotifyResponse r = new ControlFleetNotifyResponse();
        r.ok = true;
        r.delivered = delivered;
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getDelivered() {
        return delivered;
    }

    public void setDelivered(int delivered) {
        this.delivered = delivered;
    }
}

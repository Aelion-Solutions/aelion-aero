package com.aelion.aero.common.control;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlHealthResponse {

    private boolean ok = true;
    private String plugin = AeroConstants.NAME;
    private String version = AeroVersion.VERSION;

    public ControlHealthResponse() {
    }

    public static ControlHealthResponse ok() {
        return new ControlHealthResponse();
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

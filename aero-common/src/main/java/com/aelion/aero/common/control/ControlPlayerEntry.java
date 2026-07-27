package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Online player entry for {@code GET /v1/players}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlPlayerEntry {

    private String uuid;
    private String name;

    public ControlPlayerEntry() {
    }

    public ControlPlayerEntry(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

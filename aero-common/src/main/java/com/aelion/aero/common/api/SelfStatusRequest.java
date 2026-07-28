package com.aelion.aero.common.api;

/**
 * Body for {@code POST /api/aero/v1/self/status}.
 */
public final class SelfStatusRequest {

    private String motd;
    private Integer currentPlayers;
    private Integer maxPlayers;

    public SelfStatusRequest() {
    }

    public SelfStatusRequest(String motd, Integer currentPlayers, Integer maxPlayers) {
        this.motd = motd;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public Integer getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}

package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body for {@code POST /v1/players/transfer}.
 * Prefer {@code proxyServerName} when known; otherwise panel id/name fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlTransferRequest {

    private String uuid;
    private String proxyServerName;
    private String serverId;
    private String serverName;
    private String groupId;
    private String groupName;

    public ControlTransferRequest() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getProxyServerName() {
        return proxyServerName;
    }

    public void setProxyServerName(String proxyServerName) {
        this.proxyServerName = proxyServerName;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}

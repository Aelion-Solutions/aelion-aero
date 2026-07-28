package com.aelion.aero.common.api;

/**
 * HTTPS client for Aelion Cloud panel (Aero-facing REST).
 */
public interface PanelClient {

    PanelHealthResponse ping() throws PanelNotConfiguredException, PanelApiException;

    ServerInfoResponse getServerInfo() throws PanelNotConfiguredException, PanelApiException;

    java.util.List<ServerInfoResponse> listServers() throws PanelNotConfiguredException, PanelApiException;

    java.util.List<GroupInfoResponse> listGroups() throws PanelNotConfiguredException, PanelApiException;

    CreateServerResponse createServer(CreateServerRequest request)
            throws PanelNotConfiguredException, PanelApiException;

    CreateGroupResponse createGroup(CreateGroupRequest request)
            throws PanelNotConfiguredException, PanelApiException;

    /**
     * Push live MOTD / player counts for the calling Aero server.
     */
    void postSelfStatus(SelfStatusRequest request) throws PanelNotConfiguredException, PanelApiException;
}

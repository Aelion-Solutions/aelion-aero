package com.aelion.aero.common.api;

/**
 * HTTPS client for Aelion Cloud panel (Aero-facing REST).
 */
public interface PanelClient {

    PanelHealthResponse ping() throws PanelNotConfiguredException, PanelApiException;

    ServerInfoResponse getServerInfo() throws PanelNotConfiguredException, PanelApiException;

    CreateServerResponse createServer(CreateServerRequest request)
            throws PanelNotConfiguredException, PanelApiException;

    CreateGroupResponse createGroup(CreateGroupRequest request)
            throws PanelNotConfiguredException, PanelApiException;
}

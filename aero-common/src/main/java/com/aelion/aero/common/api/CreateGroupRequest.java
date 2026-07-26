package com.aelion.aero.common.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Aligns with Aelion Cloud {@code RestCreateGroupRequest}.
 * Bootstrap until OpenAPI codegen replaces this package.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CreateGroupRequest {

    private String name;
    private String description;
    private String type;
    private String templateId;
    private GroupScaling scaling;
    private GroupServerConfig serverConfig;
    private Map<String, Object> nodeSelector;
    private String playerDistribution;
    private String staticTemplatePolicy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public GroupScaling getScaling() {
        return scaling;
    }

    public void setScaling(GroupScaling scaling) {
        this.scaling = scaling;
    }

    public GroupServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(GroupServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public Map<String, Object> getNodeSelector() {
        return nodeSelector;
    }

    public void setNodeSelector(Map<String, Object> nodeSelector) {
        this.nodeSelector = nodeSelector;
    }

    public String getPlayerDistribution() {
        return playerDistribution;
    }

    public void setPlayerDistribution(String playerDistribution) {
        this.playerDistribution = playerDistribution;
    }

    public String getStaticTemplatePolicy() {
        return staticTemplatePolicy;
    }

    public void setStaticTemplatePolicy(String staticTemplatePolicy) {
        this.staticTemplatePolicy = staticTemplatePolicy;
    }
}

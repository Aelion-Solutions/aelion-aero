package com.aelion.aero.common.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Aligns with Aelion Cloud {@code CliCreateServerRequest} (REST camelCase).
 * Bootstrap until OpenAPI codegen replaces this package.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CreateServerRequest {

    private String name;
    private String type;
    private String software;
    private String version;
    private String nodeId;
    private String groupId;
    private String templateId;
    private String template;
    private String role;
    private String customJarUrl;
    private Integer memory;
    private Integer disk;
    private Integer cores;
    private Integer port;
    private Integer maxPlayers;
    private String javaPath;
    private List<String> jvmArgs;
    private List<String> serverArgs;
    private Map<String, String> environment;
    private Boolean autoStart;
    private Boolean autoStartOnBoot;
    private Boolean restartOnCrash;
    private Integer maxRestarts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCustomJarUrl() {
        return customJarUrl;
    }

    public void setCustomJarUrl(String customJarUrl) {
        this.customJarUrl = customJarUrl;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getDisk() {
        return disk;
    }

    public void setDisk(Integer disk) {
        this.disk = disk;
    }

    public Integer getCores() {
        return cores;
    }

    public void setCores(Integer cores) {
        this.cores = cores;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public List<String> getServerArgs() {
        return serverArgs;
    }

    public void setServerArgs(List<String> serverArgs) {
        this.serverArgs = serverArgs;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public Boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart) {
        this.autoStart = autoStart;
    }

    public Boolean getAutoStartOnBoot() {
        return autoStartOnBoot;
    }

    public void setAutoStartOnBoot(Boolean autoStartOnBoot) {
        this.autoStartOnBoot = autoStartOnBoot;
    }

    public Boolean getRestartOnCrash() {
        return restartOnCrash;
    }

    public void setRestartOnCrash(Boolean restartOnCrash) {
        this.restartOnCrash = restartOnCrash;
    }

    public Integer getMaxRestarts() {
        return maxRestarts;
    }

    public void setMaxRestarts(Integer maxRestarts) {
        this.maxRestarts = maxRestarts;
    }
}

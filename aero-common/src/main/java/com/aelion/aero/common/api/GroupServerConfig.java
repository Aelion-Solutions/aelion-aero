package com.aelion.aero.common.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Aligns with Aelion Cloud {@code RestGroupServerConfig}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GroupServerConfig {

    private int memory;
    private int maxPlayers;
    private String version;
    private String software;
    private Integer disk;
    private Integer cores;
    private String customJarUrl;
    private Boolean proxyMode;
    private String proxyServerId;
    private String proxyRole;
    private String javaPath;
    private List<String> jvmArgs;
    private List<String> serverArgs;
    private Map<String, String> environment;
    private Boolean autoStart;
    private Boolean restartOnCrash;
    private Integer maxRestarts;

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
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

    public String getCustomJarUrl() {
        return customJarUrl;
    }

    public void setCustomJarUrl(String customJarUrl) {
        this.customJarUrl = customJarUrl;
    }

    public Boolean getProxyMode() {
        return proxyMode;
    }

    public void setProxyMode(Boolean proxyMode) {
        this.proxyMode = proxyMode;
    }

    public String getProxyServerId() {
        return proxyServerId;
    }

    public void setProxyServerId(String proxyServerId) {
        this.proxyServerId = proxyServerId;
    }

    public String getProxyRole() {
        return proxyRole;
    }

    public void setProxyRole(String proxyRole) {
        this.proxyRole = proxyRole;
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

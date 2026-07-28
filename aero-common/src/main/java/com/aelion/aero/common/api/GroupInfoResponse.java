package com.aelion.aero.common.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GroupInfoResponse {

    private String id;
    private String name;
    private String status;
    private int currentPlayers;
    private int maxPlayers;
    private int memberCount;
    private String liveStatus;
    private List<GroupMemberInfo> members = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getLiveStatus() {
        return liveStatus;
    }

    public void setLiveStatus(String liveStatus) {
        this.liveStatus = liveStatus;
    }

    public List<GroupMemberInfo> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberInfo> members) {
        this.members = members == null ? new ArrayList<>() : members;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GroupMemberInfo {
        private String id;
        private String name;
        private String proxyName;
        private String liveStatus;
        private int currentPlayers;
        private int maxPlayers;
        private boolean joinable;
        private String motd;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProxyName() {
            return proxyName;
        }

        public void setProxyName(String proxyName) {
            this.proxyName = proxyName;
        }

        public String getLiveStatus() {
            return liveStatus;
        }

        public void setLiveStatus(String liveStatus) {
            this.liveStatus = liveStatus;
        }

        public int getCurrentPlayers() {
            return currentPlayers;
        }

        public void setCurrentPlayers(int currentPlayers) {
            this.currentPlayers = currentPlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public boolean isJoinable() {
            return joinable;
        }

        public void setJoinable(boolean joinable) {
            this.joinable = joinable;
        }

        public String getMotd() {
            return motd;
        }

        public void setMotd(String motd) {
            this.motd = motd;
        }
    }
}

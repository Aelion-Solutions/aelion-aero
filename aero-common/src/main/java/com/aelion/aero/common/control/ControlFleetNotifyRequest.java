package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Body for {@code POST /v1/fleet-notify} (daemon → proxy Aero).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlFleetNotifyRequest {

    private List<ControlFleetNotifyEvent> events = new ArrayList<>();

    public ControlFleetNotifyRequest() {
    }

    public List<ControlFleetNotifyEvent> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }

    public void setEvents(List<ControlFleetNotifyEvent> events) {
        this.events = events;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ControlFleetNotifyEvent {
        private String id;
        private String name;
        private String status;
        private String groupId;
        private String groupName;

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
}

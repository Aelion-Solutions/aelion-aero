package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Full desired backend set (replace semantics, not patch).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BackendRegistry {

    private List<BackendEntry> backends = new ArrayList<>();

    public BackendRegistry() {
    }

    public BackendRegistry(List<BackendEntry> backends) {
        setBackends(backends);
    }

    public List<BackendEntry> getBackends() {
        return backends;
    }

    public void setBackends(List<BackendEntry> backends) {
        this.backends = backends == null ? new ArrayList<>() : new ArrayList<>(backends);
    }

    public List<BackendEntry> validBackends() {
        List<BackendEntry> valid = new ArrayList<>();
        for (BackendEntry entry : backends) {
            if (entry != null && entry.isValid()) {
                valid.add(entry);
            }
        }
        return Collections.unmodifiableList(valid);
    }
}

package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * One proxy backend target.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BackendEntry {

    private String name;
    private String address;
    private ProxyBackendRole role = ProxyBackendRole.BACKEND;

    public BackendEntry() {
    }

    public BackendEntry(String name, String address, ProxyBackendRole role) {
        this.name = name;
        this.address = address;
        this.role = role == null ? ProxyBackendRole.BACKEND : role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ProxyBackendRole getRole() {
        return role;
    }

    public void setRole(ProxyBackendRole role) {
        this.role = role == null ? ProxyBackendRole.BACKEND : role;
    }

    public boolean isValid() {
        return name != null && !name.isBlank() && address != null && !address.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackendEntry that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && Objects.equals(address, that.address)
                && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, role);
    }
}

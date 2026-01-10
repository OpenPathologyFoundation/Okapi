package com.okapi.auth.model;

public enum Role {
    PATHOLOGIST,
    TECHNICIAN,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}

package com.okapi.auth.model;

public enum Role {
    ADMIN,
    PATHOLOGIST,
    TECHNICIAN,
    RESIDENT,
    FELLOW,
    HISTO_TECH,
    CYTO_TECH,
    RESEARCHER,
    RESEARCH_ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}

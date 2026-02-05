package com.controlphonedesk.rbac.dto;

import com.controlphonedesk.rbac.entity.UserStatus;

public class UserRef {
    private Long id;
    private String username;
    private UserStatus status;
    private boolean superAdmin;

    public UserRef() {}

    public UserRef(Long id, String username, UserStatus status, boolean superAdmin) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.superAdmin = superAdmin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }
}

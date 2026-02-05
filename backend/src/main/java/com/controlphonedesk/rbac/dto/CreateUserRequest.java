package com.controlphonedesk.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateUserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private List<Long> roleIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}

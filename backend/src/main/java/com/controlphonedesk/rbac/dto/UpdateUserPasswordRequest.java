package com.controlphonedesk.rbac.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateUserPasswordRequest {
    @NotBlank
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

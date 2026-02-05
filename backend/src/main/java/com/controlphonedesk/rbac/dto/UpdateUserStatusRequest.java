package com.controlphonedesk.rbac.dto;

import com.controlphonedesk.rbac.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateUserStatusRequest {
    @NotNull
    private UserStatus status;

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}

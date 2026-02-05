package com.controlphonedesk.rbac.dto;

import jakarta.validation.constraints.NotBlank;

public class UserGroupUpsertRequest {
    @NotBlank
    private String name;
    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

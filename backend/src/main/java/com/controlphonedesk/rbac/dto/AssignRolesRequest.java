package com.controlphonedesk.rbac.dto;

import java.util.List;

public class AssignRolesRequest {
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}

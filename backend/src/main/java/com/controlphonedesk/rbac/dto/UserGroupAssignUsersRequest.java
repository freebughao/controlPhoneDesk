package com.controlphonedesk.rbac.dto;

import java.util.List;

public class UserGroupAssignUsersRequest {
    private List<Long> userIds;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}

package com.controlphonedesk.rbac.dto;

import java.util.List;

public class UserGroupAssignDeviceGroupsRequest {
    private List<Long> deviceGroupIds;

    public List<Long> getDeviceGroupIds() {
        return deviceGroupIds;
    }

    public void setDeviceGroupIds(List<Long> deviceGroupIds) {
        this.deviceGroupIds = deviceGroupIds;
    }
}

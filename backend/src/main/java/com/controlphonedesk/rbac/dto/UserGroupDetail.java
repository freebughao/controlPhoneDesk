package com.controlphonedesk.rbac.dto;

import com.controlphonedesk.device.dto.DeviceGroupInfo;
import java.util.List;

public class UserGroupDetail {
    private Long id;
    private String name;
    private String remark;
    private List<UserRef> users;
    private List<DeviceGroupInfo> deviceGroups;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<UserRef> getUsers() {
        return users;
    }

    public void setUsers(List<UserRef> users) {
        this.users = users;
    }

    public List<DeviceGroupInfo> getDeviceGroups() {
        return deviceGroups;
    }

    public void setDeviceGroups(List<DeviceGroupInfo> deviceGroups) {
        this.deviceGroups = deviceGroups;
    }
}

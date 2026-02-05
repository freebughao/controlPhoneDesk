package com.controlphonedesk.rbac.dto;

public class UserGroupInfo {
    private Long id;
    private String name;
    private String remark;
    private int userCount;
    private int deviceGroupCount;

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

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getDeviceGroupCount() {
        return deviceGroupCount;
    }

    public void setDeviceGroupCount(int deviceGroupCount) {
        this.deviceGroupCount = deviceGroupCount;
    }
}

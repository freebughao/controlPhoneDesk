package com.controlphonedesk.device.dto;

import jakarta.validation.constraints.NotBlank;

public class GroupAddDeviceRequest {
    @NotBlank
    private String deviceId;
    private String alias;
    private String remark;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

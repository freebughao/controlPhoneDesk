package com.controlphonedesk.device.dto;

import java.util.List;

public class DeviceDto {
    private String deviceId;
    private String state;
    private String alias;
    private String remark;
    private String manufacturer;
    private String model;
    private String androidRelease;
    private String androidSdk;
    private List<GroupRef> groups;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAndroidRelease() {
        return androidRelease;
    }

    public void setAndroidRelease(String androidRelease) {
        this.androidRelease = androidRelease;
    }

    public String getAndroidSdk() {
        return androidSdk;
    }

    public void setAndroidSdk(String androidSdk) {
        this.androidSdk = androidSdk;
    }

    public List<GroupRef> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupRef> groups) {
        this.groups = groups;
    }
}

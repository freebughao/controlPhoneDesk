package com.controlphonedesk.device;

import java.util.ArrayList;
import java.util.List;

public class DeviceInfo {
    private String udid;
    private String state;
    private String model;
    private String manufacturer;
    private String androidRelease;
    private String androidSdk;
    private String abi;
    private List<NetInterfaceInfo> interfaces = new ArrayList<>();

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = udid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
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

    public String getAbi() {
        return abi;
    }

    public void setAbi(String abi) {
        this.abi = abi;
    }

    public List<NetInterfaceInfo> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<NetInterfaceInfo> interfaces) {
        this.interfaces = interfaces;
    }
}

package com.controlphonedesk.adb;

import java.util.HashMap;
import java.util.Map;

public class AdbDevice {
    private final String udid;
    private final String state;
    private final Map<String, String> fields = new HashMap<>();

    public AdbDevice(String udid, String state) {
        this.udid = udid;
        this.state = state;
    }

    public String getUdid() {
        return udid;
    }

    public String getState() {
        return state;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}

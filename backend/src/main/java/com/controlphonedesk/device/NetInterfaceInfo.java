package com.controlphonedesk.device;

public class NetInterfaceInfo {
    private String name;
    private String ipv4;

    public NetInterfaceInfo() {
    }

    public NetInterfaceInfo(String name, String ipv4) {
        this.name = name;
        this.ipv4 = ipv4;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpv4() {
        return ipv4;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }
}

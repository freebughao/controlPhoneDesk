package com.controlphonedesk.device;

import com.controlphonedesk.adb.AdbDevice;
import com.controlphonedesk.adb.AdbService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
    private final AdbService adbService;

    public DeviceService(AdbService adbService) {
        this.adbService = adbService;
    }

    /**
     * 聚合 adb 信息 + getprop + 网络接口为前端展示结构。
     */
    public List<DeviceInfo> listDevices() {
        List<DeviceInfo> devices = new ArrayList<>();
        try {
            List<AdbDevice> adbDevices = adbService.listDevices();
            for (AdbDevice adbDevice : adbDevices) {
                DeviceInfo info = new DeviceInfo();
                info.setUdid(adbDevice.getUdid());
                info.setState(adbDevice.getState());
                info.setModel(adbDevice.getFields().getOrDefault("model", ""));

                if ("device".equals(adbDevice.getState())) {
                    Map<String, String> props = adbService.getProps(adbDevice.getUdid());
                    info.setManufacturer(props.getOrDefault("ro.product.manufacturer", ""));
                    info.setModel(props.getOrDefault("ro.product.model", info.getModel()));
                    info.setAndroidRelease(props.getOrDefault("ro.build.version.release", ""));
                    info.setAndroidSdk(props.getOrDefault("ro.build.version.sdk", ""));
                    info.setAbi(props.getOrDefault("ro.product.cpu.abi", ""));
                    info.setInterfaces(adbService.getInterfaces(adbDevice.getUdid()));
                }
                devices.add(info);
            }
        } catch (Exception error) {
            throw new RuntimeException("Failed to list devices: " + error.getMessage(), error);
        }
        return devices;
    }
}

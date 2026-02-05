package com.controlphonedesk.web;

import com.controlphonedesk.auth.RequirePermission;
import com.controlphonedesk.device.DeviceInfo;
import com.controlphonedesk.device.DeviceService;
import com.controlphonedesk.device.dto.DeviceDto;
import com.controlphonedesk.device.dto.DeviceUpdateRequest;
import com.controlphonedesk.device.service.DeviceRegistryService;
import com.controlphonedesk.rbac.seed.DefaultPermissions;
import com.controlphonedesk.rbac.service.UserDeviceScope;
import com.controlphonedesk.rbac.service.UserDeviceScopeService;
import com.controlphonedesk.scrcpy.ScrcpyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.context.SecurityContextHolder;
import com.controlphonedesk.auth.UserPrincipal;

@RestController
@RequestMapping("/api")
public class DeviceController {
    private final DeviceService deviceService;
    private final ScrcpyService scrcpyService;
    private final DeviceRegistryService deviceRegistryService;
    private final UserDeviceScopeService userDeviceScopeService;

    public DeviceController(
        DeviceService deviceService,
        ScrcpyService scrcpyService,
        DeviceRegistryService deviceRegistryService,
        UserDeviceScopeService userDeviceScopeService
    ) {
        this.deviceService = deviceService;
        this.scrcpyService = scrcpyService;
        this.deviceRegistryService = deviceRegistryService;
        this.userDeviceScopeService = userDeviceScopeService;
    }

    /**
     * 获取当前 adb 识别到的设备列表。
     */
    @GetMapping("/devices/adb")
    @RequirePermission(DefaultPermissions.DEVICE_ADB_LIST)
    public List<DeviceInfo> listAdbDevices() {
        return deviceService.listDevices();
    }

    /**
     * 获取设备列表（合并本地元数据）。
     */
    @GetMapping("/devices")
    @RequirePermission(DefaultPermissions.DEVICE_LIST)
    public List<DeviceDto> listDevices() {
        UserPrincipal principal = getCurrentPrincipal();
        if (shouldApplyScope(principal)) {
            UserDeviceScope scope = userDeviceScopeService.getScope(principal.getId());
            return deviceRegistryService.listMergedDevices(scope);
        }
        return deviceRegistryService.listMergedDevices();
    }

    /**
     * 更新设备别名/备注。
     */
    @PutMapping("/devices/{deviceId}")
    @RequirePermission(DefaultPermissions.DEVICE_UPDATE)
    public org.springframework.http.ResponseEntity<DeviceDto> updateDevice(
        @PathVariable String deviceId,
        @RequestBody DeviceUpdateRequest request
    ) {
        deviceRegistryService.updateMetadata(deviceId, request.getAlias(), request.getRemark());
        DeviceDto updated = deviceRegistryService.listMergedDevices().stream()
            .filter(device -> deviceId.equals(device.getDeviceId()))
            .findFirst()
            .orElse(null);
        if (updated == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        return org.springframework.http.ResponseEntity.ok(updated);
    }

    /**
     * 启动指定设备的 scrcpy server（若已启动则直接返回 PID）。
     */
    @PostMapping("/devices/{udid}/scrcpy/start")
    @RequirePermission(DefaultPermissions.DEVICE_CONNECT)
    public org.springframework.http.ResponseEntity<Map<String, Object>> startScrcpy(@PathVariable String udid) throws Exception {
        UserPrincipal principal = getCurrentPrincipal();
        if (shouldApplyScope(principal)) {
            if (!userDeviceScopeService.canAccessDevice(principal.getId(), udid)) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "无权限访问该设备"));
            }
        }
        // 强制重启，确保新连接能拿到 SPS/PPS/IDR
        int pid = scrcpyService.ensureServerRunning(udid, true);
        Map<String, Object> response = new HashMap<>();
        response.put("udid", udid);
        response.put("pid", pid);
        return org.springframework.http.ResponseEntity.ok(response);
    }

    private UserPrincipal getCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private boolean shouldApplyScope(UserPrincipal principal) {
        if (principal == null || principal.isSuperAdmin()) {
            return false;
        }
        if (principal.getPermissions() == null || principal.getPermissions().isEmpty()) {
            return true;
        }
        return !(principal.getPermissions().contains(DefaultPermissions.DEVICE_UPDATE)
            || principal.getPermissions().contains(DefaultPermissions.GROUP_CREATE)
            || principal.getPermissions().contains(DefaultPermissions.GROUP_UPDATE)
            || principal.getPermissions().contains(DefaultPermissions.GROUP_DELETE));
    }
}

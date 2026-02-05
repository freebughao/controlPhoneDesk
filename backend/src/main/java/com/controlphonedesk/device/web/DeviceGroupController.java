package com.controlphonedesk.device.web;

import com.controlphonedesk.auth.RequirePermission;
import com.controlphonedesk.auth.UserPrincipal;
import com.controlphonedesk.device.dto.DeviceGroupDetail;
import com.controlphonedesk.device.dto.DeviceGroupInfo;
import com.controlphonedesk.device.dto.GroupAddDeviceRequest;
import com.controlphonedesk.device.dto.GroupUpsertRequest;
import com.controlphonedesk.device.service.DeviceGroupService;
import com.controlphonedesk.rbac.seed.DefaultPermissions;
import com.controlphonedesk.rbac.service.UserDeviceScope;
import com.controlphonedesk.rbac.service.UserDeviceScopeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class DeviceGroupController {
    private final DeviceGroupService deviceGroupService;
    private final UserDeviceScopeService userDeviceScopeService;

    public DeviceGroupController(DeviceGroupService deviceGroupService, UserDeviceScopeService userDeviceScopeService) {
        this.deviceGroupService = deviceGroupService;
        this.userDeviceScopeService = userDeviceScopeService;
    }

    @GetMapping
    @RequirePermission(DefaultPermissions.GROUP_LIST)
    public List<DeviceGroupInfo> listGroups() {
        UserPrincipal principal = getCurrentPrincipal();
        if (shouldApplyScope(principal)) {
            UserDeviceScope scope = userDeviceScopeService.getScope(principal.getId());
            Set<Long> allowed = scope.deviceGroupIds();
            return deviceGroupService.listGroups().stream()
                .filter(group -> allowed.contains(group.getId()))
                .toList();
        }
        return deviceGroupService.listGroups();
    }

    @GetMapping("/{id}")
    @RequirePermission(DefaultPermissions.GROUP_LIST)
    public ResponseEntity<DeviceGroupDetail> getGroup(@PathVariable Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        if (shouldApplyScope(principal)) {
            UserDeviceScope scope = userDeviceScopeService.getScope(principal.getId());
            if (!scope.deviceGroupIds().contains(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        DeviceGroupDetail detail = deviceGroupService.getGroupDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    @PostMapping
    @RequirePermission(DefaultPermissions.GROUP_CREATE)
    public ResponseEntity<DeviceGroupInfo> createGroup(@Valid @RequestBody GroupUpsertRequest request) {
        var group = deviceGroupService.createGroup(request.getName(), request.getRemark());
        DeviceGroupInfo info = new DeviceGroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setRemark(group.getRemark());
        info.setDeviceCount(group.getDevices().size());
        return ResponseEntity.status(HttpStatus.CREATED).body(info);
    }

    @PutMapping("/{id}")
    @RequirePermission(DefaultPermissions.GROUP_UPDATE)
    public ResponseEntity<DeviceGroupInfo> updateGroup(@PathVariable Long id, @Valid @RequestBody GroupUpsertRequest request) {
        var group = deviceGroupService.updateGroup(id, request.getName(), request.getRemark());
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        DeviceGroupInfo info = new DeviceGroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setRemark(group.getRemark());
        info.setDeviceCount(group.getDevices().size());
        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(DefaultPermissions.GROUP_DELETE)
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        deviceGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/devices")
    @RequirePermission(DefaultPermissions.GROUP_DEVICE_LIST)
    public ResponseEntity<DeviceGroupDetail> listGroupDevices(@PathVariable Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        if (shouldApplyScope(principal)) {
            UserDeviceScope scope = userDeviceScopeService.getScope(principal.getId());
            if (!scope.deviceGroupIds().contains(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        DeviceGroupDetail detail = deviceGroupService.getGroupDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/devices")
    @RequirePermission(DefaultPermissions.GROUP_DEVICE_ADD)
    public ResponseEntity<DeviceGroupDetail> addDevice(@PathVariable Long id, @Valid @RequestBody GroupAddDeviceRequest request) {
        deviceGroupService.addDeviceToGroup(id, request.getDeviceId(), request.getAlias(), request.getRemark());
        DeviceGroupDetail detail = deviceGroupService.getGroupDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @DeleteMapping("/{id}/devices/{deviceId}")
    @RequirePermission(DefaultPermissions.GROUP_DEVICE_REMOVE)
    public ResponseEntity<Void> removeDevice(@PathVariable Long id, @PathVariable String deviceId) {
        deviceGroupService.removeDeviceFromGroup(id, deviceId);
        return ResponseEntity.noContent().build();
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

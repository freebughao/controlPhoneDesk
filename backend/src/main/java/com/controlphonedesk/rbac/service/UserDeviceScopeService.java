package com.controlphonedesk.rbac.service;

import com.controlphonedesk.device.entity.DeviceEntity;
import com.controlphonedesk.device.entity.DeviceGroup;
import com.controlphonedesk.rbac.entity.UserGroup;
import com.controlphonedesk.rbac.repo.UserGroupRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeviceScopeService {
    private final UserGroupRepository userGroupRepository;

    public UserDeviceScopeService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional(readOnly = true)
    public UserDeviceScope getScope(Long userId) {
        List<UserGroup> groups = userGroupRepository.findByUsers_Id(userId);
        Set<Long> deviceGroupIds = new HashSet<>();
        Set<String> deviceIds = new HashSet<>();
        for (UserGroup group : groups) {
            for (DeviceGroup deviceGroup : group.getDeviceGroups()) {
                deviceGroupIds.add(deviceGroup.getId());
                for (DeviceEntity device : deviceGroup.getDevices()) {
                    deviceIds.add(device.getDeviceId());
                }
            }
        }
        return new UserDeviceScope(deviceGroupIds, deviceIds);
    }

    @Transactional(readOnly = true)
    public boolean canAccessDevice(Long userId, String deviceId) {
        return getScope(userId).deviceIds().contains(deviceId);
    }
}

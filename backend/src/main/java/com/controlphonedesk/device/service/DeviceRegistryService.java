package com.controlphonedesk.device.service;

import com.controlphonedesk.device.DeviceInfo;
import com.controlphonedesk.device.DeviceService;
import com.controlphonedesk.device.dto.DeviceDto;
import com.controlphonedesk.device.dto.GroupRef;
import com.controlphonedesk.device.entity.DeviceEntity;
import com.controlphonedesk.device.entity.DeviceGroup;
import com.controlphonedesk.device.repo.DeviceGroupRepository;
import com.controlphonedesk.device.repo.DeviceRepository;
import com.controlphonedesk.rbac.service.UserDeviceScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceRegistryService {
    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceService deviceService;

    public DeviceRegistryService(
        DeviceRepository deviceRepository,
        DeviceGroupRepository deviceGroupRepository,
        DeviceService deviceService
    ) {
        this.deviceRepository = deviceRepository;
        this.deviceGroupRepository = deviceGroupRepository;
        this.deviceService = deviceService;
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> listMergedDevices() {
        List<DeviceInfo> adbDevices = deviceService.listDevices();
        Map<String, DeviceInfo> adbMap = adbDevices.stream()
            .collect(Collectors.toMap(DeviceInfo::getUdid, info -> info, (a, b) -> a));

        Map<String, DeviceEntity> entityMap = new HashMap<>();
        deviceRepository.findAll().forEach(entity -> entityMap.put(entity.getDeviceId(), entity));

        List<DeviceDto> result = new ArrayList<>();
        for (DeviceInfo adb : adbDevices) {
            DeviceEntity entity = entityMap.remove(adb.getUdid());
            result.add(toDeviceDto(entity, adb));
        }

        entityMap.values().forEach(entity -> result.add(toDeviceDto(entity, null)));
        return result;
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> listMergedDevices(UserDeviceScope scope) {
        if (scope == null || scope.isEmpty()) {
            return List.of();
        }
        List<DeviceDto> devices = listMergedDevices();
        Set<String> allowedDeviceIds = scope.deviceIds();
        Set<Long> allowedGroupIds = scope.deviceGroupIds();
        return devices.stream()
            .filter(device -> allowedDeviceIds.contains(device.getDeviceId()))
            .map(device -> filterGroups(device, allowedGroupIds))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> listGroupDevices(Long groupId) {
        DeviceGroup group = deviceGroupRepository.findWithDevicesById(groupId).orElse(null);
        if (group == null) {
            return List.of();
        }
        Map<String, DeviceInfo> adbMap = deviceService.listDevices().stream()
            .collect(Collectors.toMap(DeviceInfo::getUdid, info -> info, (a, b) -> a));
        List<DeviceDto> devices = new ArrayList<>();
        for (DeviceEntity entity : group.getDevices()) {
            devices.add(toDeviceDto(entity, adbMap.get(entity.getDeviceId())));
        }
        return devices;
    }

    @Transactional
    public DeviceEntity upsertDevice(String deviceId, String alias, String remark) {
        Optional<DeviceEntity> existing = deviceRepository.findByDeviceId(deviceId);
        DeviceEntity entity = existing.orElseGet(DeviceEntity::new);
        entity.setDeviceId(deviceId);
        if (alias != null) {
            entity.setAlias(alias);
        }
        if (remark != null) {
            entity.setRemark(remark);
        }
        return deviceRepository.save(entity);
    }

    @Transactional
    public DeviceEntity updateMetadata(String deviceId, String alias, String remark) {
        DeviceEntity entity = deviceRepository.findByDeviceId(deviceId).orElseGet(DeviceEntity::new);
        entity.setDeviceId(deviceId);
        entity.setAlias(alias);
        entity.setRemark(remark);
        return deviceRepository.save(entity);
    }

    private DeviceDto toDeviceDto(DeviceEntity entity, DeviceInfo adb) {
        DeviceDto dto = new DeviceDto();
        if (adb != null) {
            dto.setDeviceId(adb.getUdid());
            dto.setState(adb.getState());
            dto.setManufacturer(adb.getManufacturer());
            dto.setModel(adb.getModel());
            dto.setAndroidRelease(adb.getAndroidRelease());
            dto.setAndroidSdk(adb.getAndroidSdk());
        } else if (entity != null) {
            dto.setDeviceId(entity.getDeviceId());
            dto.setState("offline");
        }
        if (entity != null) {
            dto.setAlias(entity.getAlias());
            dto.setRemark(entity.getRemark());
            dto.setGroups(entity.getGroups().stream()
                .map(group -> new GroupRef(group.getId(), group.getName()))
                .toList());
        } else {
            dto.setGroups(List.of());
        }
        return dto;
    }

    private DeviceDto filterGroups(DeviceDto dto, Set<Long> allowedGroupIds) {
        if (allowedGroupIds == null || allowedGroupIds.isEmpty()) {
            dto.setGroups(List.of());
            return dto;
        }
        dto.setGroups(dto.getGroups().stream()
            .filter(group -> allowedGroupIds.contains(group.getId()))
            .toList());
        return dto;
    }
}

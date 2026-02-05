package com.controlphonedesk.device.service;

import com.controlphonedesk.device.dto.DeviceGroupDetail;
import com.controlphonedesk.device.dto.DeviceGroupInfo;
import com.controlphonedesk.device.entity.DeviceEntity;
import com.controlphonedesk.device.entity.DeviceGroup;
import com.controlphonedesk.device.repo.DeviceGroupRepository;
import com.controlphonedesk.device.repo.DeviceRepository;
import com.controlphonedesk.rbac.repo.UserGroupRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceGroupService {
    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceRegistryService deviceRegistryService;
    private final UserGroupRepository userGroupRepository;

    public DeviceGroupService(
        DeviceGroupRepository deviceGroupRepository,
        DeviceRepository deviceRepository,
        DeviceRegistryService deviceRegistryService,
        UserGroupRepository userGroupRepository
    ) {
        this.deviceGroupRepository = deviceGroupRepository;
        this.deviceRepository = deviceRepository;
        this.deviceRegistryService = deviceRegistryService;
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceGroupInfo> listGroups() {
        return deviceGroupRepository.findAll().stream()
            .map(group -> {
                DeviceGroupInfo info = new DeviceGroupInfo();
                info.setId(group.getId());
                info.setName(group.getName());
                info.setRemark(group.getRemark());
                info.setDeviceCount(group.getDevices().size());
                return info;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public DeviceGroupDetail getGroupDetail(Long id) {
        DeviceGroup group = deviceGroupRepository.findWithDevicesById(id).orElse(null);
        if (group == null) {
            return null;
        }
        DeviceGroupDetail detail = new DeviceGroupDetail();
        detail.setId(group.getId());
        detail.setName(group.getName());
        detail.setRemark(group.getRemark());
        detail.setDevices(deviceRegistryService.listGroupDevices(id));
        return detail;
    }

    @Transactional
    public DeviceGroup createGroup(String name, String remark) {
        DeviceGroup group = new DeviceGroup();
        group.setName(name);
        group.setRemark(remark);
        return deviceGroupRepository.save(group);
    }

    @Transactional
    public DeviceGroup updateGroup(Long id, String name, String remark) {
        Optional<DeviceGroup> existing = deviceGroupRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        DeviceGroup group = existing.get();
        if (name != null) {
            group.setName(name);
        }
        group.setRemark(remark);
        return deviceGroupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        userGroupRepository.findByDeviceGroups_Id(id).forEach(group -> {
            group.getDeviceGroups().removeIf(deviceGroup -> deviceGroup.getId().equals(id));
            userGroupRepository.save(group);
        });
        deviceGroupRepository.deleteById(id);
    }

    @Transactional
    public void addDeviceToGroup(Long groupId, String deviceId, String alias, String remark) {
        DeviceGroup group = deviceGroupRepository.findWithDevicesById(groupId).orElse(null);
        if (group == null) {
            return;
        }
        DeviceEntity device = deviceRegistryService.upsertDevice(deviceId, alias, remark);
        group.getDevices().add(device);
        deviceGroupRepository.save(group);
    }

    @Transactional
    public void removeDeviceFromGroup(Long groupId, String deviceId) {
        DeviceGroup group = deviceGroupRepository.findWithDevicesById(groupId).orElse(null);
        if (group == null) {
            return;
        }
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            group.getDevices().remove(device);
            deviceGroupRepository.save(group);
        });
    }
}

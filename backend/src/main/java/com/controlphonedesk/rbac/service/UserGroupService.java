package com.controlphonedesk.rbac.service;

import com.controlphonedesk.device.dto.DeviceGroupInfo;
import com.controlphonedesk.device.entity.DeviceGroup;
import com.controlphonedesk.device.repo.DeviceGroupRepository;
import com.controlphonedesk.rbac.dto.UserGroupDetail;
import com.controlphonedesk.rbac.dto.UserGroupInfo;
import com.controlphonedesk.rbac.dto.UserRef;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserGroup;
import com.controlphonedesk.rbac.repo.UserGroupRepository;
import com.controlphonedesk.rbac.repo.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final DeviceGroupRepository deviceGroupRepository;

    public UserGroupService(
        UserGroupRepository userGroupRepository,
        UserRepository userRepository,
        DeviceGroupRepository deviceGroupRepository
    ) {
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
        this.deviceGroupRepository = deviceGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<UserGroupInfo> listGroups() {
        return userGroupRepository.findAll().stream()
            .map(this::toInfo)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserGroupDetail getGroupDetail(Long id) {
        return userGroupRepository.findWithUsersAndDeviceGroupsById(id)
            .map(this::toDetail)
            .orElse(null);
    }

    @Transactional
    public UserGroup createGroup(String name, String remark) {
        userGroupRepository.findByName(name).ifPresent(existing -> {
            throw new com.controlphonedesk.web.error.DuplicateResourceException("用户分组名称已存在");
        });
        UserGroup group = new UserGroup();
        group.setName(name);
        group.setRemark(remark);
        return userGroupRepository.save(group);
    }

    @Transactional
    public UserGroup updateGroup(Long id, String name, String remark) {
        UserGroup group = userGroupRepository.findById(id).orElse(null);
        if (group == null) {
            return null;
        }
        userGroupRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new com.controlphonedesk.web.error.DuplicateResourceException("用户分组名称已存在");
            }
        });
        group.setName(name);
        group.setRemark(remark);
        return userGroupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        userGroupRepository.deleteById(id);
    }

    @Transactional
    public UserGroup assignUsers(Long id, List<Long> userIds) {
        UserGroup group = userGroupRepository.findWithUsersAndDeviceGroupsById(id).orElse(null);
        if (group == null) {
            return null;
        }
        List<Long> ids = userIds == null ? List.of() : userIds;
        Set<User> users = new HashSet<>(userRepository.findAllById(ids));
        group.setUsers(users);
        return userGroupRepository.save(group);
    }

    @Transactional
    public UserGroup assignDeviceGroups(Long id, List<Long> deviceGroupIds) {
        UserGroup group = userGroupRepository.findWithUsersAndDeviceGroupsById(id).orElse(null);
        if (group == null) {
            return null;
        }
        List<Long> ids = deviceGroupIds == null ? List.of() : deviceGroupIds;
        Set<DeviceGroup> deviceGroups = new HashSet<>(deviceGroupRepository.findAllById(ids));
        group.setDeviceGroups(deviceGroups);
        return userGroupRepository.save(group);
    }

    private UserGroupInfo toInfo(UserGroup group) {
        UserGroupInfo info = new UserGroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setRemark(group.getRemark());
        info.setUserCount(group.getUsers().size());
        info.setDeviceGroupCount(group.getDeviceGroups().size());
        return info;
    }

    private UserGroupDetail toDetail(UserGroup group) {
        UserGroupDetail detail = new UserGroupDetail();
        detail.setId(group.getId());
        detail.setName(group.getName());
        detail.setRemark(group.getRemark());
        detail.setUsers(group.getUsers().stream()
            .map(user -> new UserRef(user.getId(), user.getUsername(), user.getStatus(), user.isSuperAdmin()))
            .toList());
        detail.setDeviceGroups(group.getDeviceGroups().stream()
            .map(this::toDeviceGroupInfo)
            .collect(Collectors.toList()));
        return detail;
    }

    private DeviceGroupInfo toDeviceGroupInfo(DeviceGroup group) {
        DeviceGroupInfo info = new DeviceGroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setRemark(group.getRemark());
        info.setDeviceCount(group.getDevices().size());
        return info;
    }
}

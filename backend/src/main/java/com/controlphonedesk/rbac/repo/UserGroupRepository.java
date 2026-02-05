package com.controlphonedesk.rbac.repo;

import com.controlphonedesk.rbac.entity.UserGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> findByName(String name);

    @Override
    @EntityGraph(attributePaths = {"users", "deviceGroups", "deviceGroups.devices"})
    List<UserGroup> findAll();

    @EntityGraph(attributePaths = {"users", "deviceGroups", "deviceGroups.devices"})
    Optional<UserGroup> findWithUsersAndDeviceGroupsById(Long id);

    @EntityGraph(attributePaths = {"deviceGroups", "deviceGroups.devices"})
    List<UserGroup> findByUsers_Id(Long userId);

    @EntityGraph(attributePaths = {"deviceGroups", "users"})
    List<UserGroup> findByDeviceGroups_Id(Long deviceGroupId);
}

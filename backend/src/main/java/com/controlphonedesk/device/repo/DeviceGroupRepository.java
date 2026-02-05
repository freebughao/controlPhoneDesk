package com.controlphonedesk.device.repo;

import com.controlphonedesk.device.entity.DeviceGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long> {
    Optional<DeviceGroup> findByName(String name);

    @EntityGraph(attributePaths = {"devices"})
    Optional<DeviceGroup> findWithDevicesById(Long id);
}

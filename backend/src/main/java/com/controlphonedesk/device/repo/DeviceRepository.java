package com.controlphonedesk.device.repo;

import com.controlphonedesk.device.entity.DeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    Optional<DeviceEntity> findByDeviceId(String deviceId);
}

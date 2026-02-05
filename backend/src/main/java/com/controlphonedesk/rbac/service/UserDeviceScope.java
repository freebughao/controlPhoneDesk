package com.controlphonedesk.rbac.service;

import java.util.Set;

public record UserDeviceScope(Set<Long> deviceGroupIds, Set<String> deviceIds) {
    public boolean isEmpty() {
        return deviceIds == null || deviceIds.isEmpty();
    }
}

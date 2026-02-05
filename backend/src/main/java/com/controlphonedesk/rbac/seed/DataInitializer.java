package com.controlphonedesk.rbac.seed;

import com.controlphonedesk.rbac.entity.Permission;
import com.controlphonedesk.rbac.entity.Role;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserStatus;
import com.controlphonedesk.rbac.repo.PermissionRepository;
import com.controlphonedesk.rbac.repo.RoleRepository;
import com.controlphonedesk.rbac.repo.UserRepository;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
        PermissionRepository permissionRepository,
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureDataDir();
        List<DefaultPermissions.PermissionSeed> seeds = DefaultPermissions.seeds();
        for (DefaultPermissions.PermissionSeed seed : seeds) {
            permissionRepository.findByCode(seed.code()).orElseGet(() -> {
                Permission permission = new Permission();
                permission.setCode(seed.code());
                permission.setName(seed.name());
                permission.setDescription(seed.description());
                return permissionRepository.save(permission);
            });
        }

        Role adminRole = roleRepository.findByCode("admin").orElseGet(() -> {
            Role role = new Role();
            role.setCode("admin");
            role.setName("管理员");
            return roleRepository.save(role);
        });
        Role userRole = roleRepository.findByCode("user").orElseGet(() -> {
            Role role = new Role();
            role.setCode("user");
            role.setName("普通用户");
            return roleRepository.save(role);
        });

        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
        if (adminRole.getPermissions().size() != allPermissions.size()) {
            adminRole.setPermissions(allPermissions);
            roleRepository.save(adminRole);
        }

        Set<Permission> userPermissions = permissionRepository.findAll().stream()
            .filter(permission -> Set.of(
                DefaultPermissions.GROUP_LIST,
                DefaultPermissions.GROUP_DEVICE_LIST,
                DefaultPermissions.DEVICE_CONNECT
            ).contains(permission.getCode()))
            .collect(java.util.stream.Collectors.toSet());
        if (!userPermissions.isEmpty()) {
            userRole.setPermissions(userPermissions);
            roleRepository.save(userRole);
        }

        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User user = new User();
            user.setUsername("admin");
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            user.setStatus(UserStatus.ACTIVE);
            user.setSuperAdmin(true);
            return userRepository.save(user);
        });
        boolean adminChanged = false;
        if (!adminUser.isSuperAdmin()) {
            adminUser.setSuperAdmin(true);
            adminChanged = true;
        }
        if (adminUser.getStatus() != UserStatus.ACTIVE) {
            adminUser.setStatus(UserStatus.ACTIVE);
            adminChanged = true;
        }
        if (!adminUser.getRoles().contains(adminRole)) {
            adminUser.getRoles().add(adminRole);
            adminChanged = true;
        }
        if (adminChanged) {
            userRepository.save(adminUser);
        }
    }

    private void ensureDataDir() {
        File dir = new File("./data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}

package com.controlphonedesk.rbac.service;

import com.controlphonedesk.rbac.dto.RoleInfo;
import com.controlphonedesk.rbac.dto.UserInfo;
import com.controlphonedesk.rbac.entity.Permission;
import com.controlphonedesk.rbac.entity.Role;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserStatus;
import com.controlphonedesk.rbac.repo.RoleRepository;
import com.controlphonedesk.rbac.repo.UserGroupRepository;
import com.controlphonedesk.rbac.repo.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserGroupRepository userGroupRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getUserWithRoles(Long id) {
        return userRepository.findWithRolesById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findWithRolesByUsername(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserInfo> listUsers() {
        return userRepository.findAll().stream()
            .map(this::toUserInfo)
            .toList();
    }

    @Transactional
    public User createUser(String username, String password, List<Long> roleIds) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new com.controlphonedesk.web.error.DuplicateResourceException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> roles = roleIds.stream()
                .map(roleRepository::findById)
                .map(optional -> optional.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        User saved = userRepository.save(user);
        return userRepository.findWithRolesById(saved.getId()).orElse(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        userGroupRepository.findByUsers_Id(id).forEach(group -> {
            group.getUsers().removeIf(user -> user.getId().equals(id));
            userGroupRepository.save(group);
        });
        userRepository.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, UserStatus status) {
        userRepository.findById(id).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }

    @Transactional
    public void updatePassword(Long id, String password) {
        userRepository.findById(id).ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(user);
        });
    }

    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        userRepository.findById(id).ifPresent(user -> {
            List<Long> ids = roleIds == null ? List.of() : roleIds;
            Set<Role> roles = ids.stream()
                .map(roleRepository::findById)
                .map(optional -> optional.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            user.setRoles(roles);
            userRepository.save(user);
        });
    }

    public Set<String> getPermissionCodes(User user) {
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());
    }

    public UserInfo toUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setStatus(user.getStatus());
        info.setCreatedAt(user.getCreatedAt());
        info.setSuperAdmin(user.isSuperAdmin());
        info.setRoles(user.getRoles().stream()
            .map(role -> new RoleInfo(role.getId(), role.getCode(), role.getName()))
            .toList());
        info.setPermissions(getPermissionCodes(user).stream().sorted().toList());
        return info;
    }
}

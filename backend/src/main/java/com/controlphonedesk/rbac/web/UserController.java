package com.controlphonedesk.rbac.web;

import com.controlphonedesk.auth.RequirePermission;
import com.controlphonedesk.auth.UserPrincipal;
import com.controlphonedesk.rbac.dto.AssignRolesRequest;
import com.controlphonedesk.rbac.dto.CreateUserRequest;
import com.controlphonedesk.rbac.dto.UpdateUserPasswordRequest;
import com.controlphonedesk.rbac.dto.UpdateUserStatusRequest;
import com.controlphonedesk.rbac.dto.UserInfo;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserStatus;
import com.controlphonedesk.rbac.seed.DefaultPermissions;
import com.controlphonedesk.rbac.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RequirePermission(DefaultPermissions.USER_LIST)
    public List<UserInfo> listUsers() {
        return userService.listUsers();
    }

    @PostMapping
    @RequirePermission(DefaultPermissions.USER_CREATE)
    public ResponseEntity<UserInfo> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword(), request.getRoleIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.toUserInfo(user));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(DefaultPermissions.USER_DELETE)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getUserWithRoles(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (user.isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (isCurrentUser(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @RequirePermission(DefaultPermissions.USER_STATUS)
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        User user = userService.getUserWithRoles(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (user.isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (isCurrentUser(id) && request.getStatus() == UserStatus.DISABLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        userService.updateStatus(id, request.getStatus());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @RequirePermission(DefaultPermissions.USER_PASSWORD)
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody UpdateUserPasswordRequest request) {
        User user = userService.getUserWithRoles(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        userService.updatePassword(id, request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles")
    @RequirePermission(DefaultPermissions.USER_ASSIGN_ROLE)
    public ResponseEntity<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        User user = userService.getUserWithRoles(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (user.isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        userService.assignRoles(id, request.getRoleIds());
        return ResponseEntity.noContent().build();
    }

    private boolean isCurrentUser(Long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getId().equals(id);
    }
}

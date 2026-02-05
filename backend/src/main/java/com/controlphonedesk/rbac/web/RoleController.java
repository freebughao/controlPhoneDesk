package com.controlphonedesk.rbac.web;

import com.controlphonedesk.auth.RequirePermission;
import com.controlphonedesk.rbac.dto.RoleDetail;
import com.controlphonedesk.rbac.entity.Permission;
import com.controlphonedesk.rbac.entity.Role;
import com.controlphonedesk.rbac.seed.DefaultPermissions;
import com.controlphonedesk.rbac.service.PermissionService;
import com.controlphonedesk.rbac.service.RoleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RoleController {
    private final RoleService roleService;
    private final PermissionService permissionService;

    public RoleController(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @GetMapping("/roles")
    @RequirePermission(DefaultPermissions.ROLE_LIST)
    public List<RoleDetail> listRoles() {
        return roleService.listRoles().stream()
            .map(this::toRoleDetail)
            .toList();
    }

    @GetMapping("/permissions")
    @RequirePermission(DefaultPermissions.PERMISSION_LIST)
    public List<Permission> listPermissions() {
        return permissionService.listPermissions();
    }

    private RoleDetail toRoleDetail(Role role) {
        RoleDetail detail = new RoleDetail();
        detail.setId(role.getId());
        detail.setCode(role.getCode());
        detail.setName(role.getName());
        detail.setPermissions(role.getPermissions().stream()
            .map(Permission::getCode)
            .sorted()
            .toList());
        return detail;
    }
}

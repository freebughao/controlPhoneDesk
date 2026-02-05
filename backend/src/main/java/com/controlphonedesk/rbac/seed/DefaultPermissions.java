package com.controlphonedesk.rbac.seed;

import java.util.List;

public final class DefaultPermissions {
    public static final String USER_LIST = "user:list";
    public static final String USER_CREATE = "user:create";
    public static final String USER_DELETE = "user:delete";
    public static final String USER_STATUS = "user:status";
    public static final String USER_PASSWORD = "user:password";
    public static final String USER_ASSIGN_ROLE = "user:assignRole";
    public static final String USER_GROUP_LIST = "usergroup:list";
    public static final String USER_GROUP_CREATE = "usergroup:create";
    public static final String USER_GROUP_UPDATE = "usergroup:update";
    public static final String USER_GROUP_DELETE = "usergroup:delete";
    public static final String USER_GROUP_ASSIGN_USER = "usergroup:user:assign";
    public static final String USER_GROUP_ASSIGN_DEVICE_GROUP = "usergroup:devicegroup:assign";

    public static final String ROLE_LIST = "role:list";
    public static final String PERMISSION_LIST = "permission:list";

    public static final String GROUP_LIST = "group:list";
    public static final String GROUP_CREATE = "group:create";
    public static final String GROUP_UPDATE = "group:update";
    public static final String GROUP_DELETE = "group:delete";
    public static final String GROUP_DEVICE_LIST = "group:device:list";
    public static final String GROUP_DEVICE_ADD = "group:device:add";
    public static final String GROUP_DEVICE_REMOVE = "group:device:remove";

    public static final String DEVICE_LIST = "device:list";
    public static final String DEVICE_ADB_LIST = "device:adb:list";
    public static final String DEVICE_UPDATE = "device:update";
    public static final String DEVICE_BIND = "device:bind";
    public static final String DEVICE_UNBIND = "device:unbind";
    public static final String DEVICE_CONNECT = "device:connect";

    public static List<PermissionSeed> seeds() {
        return List.of(
            new PermissionSeed(USER_LIST, "用户-查看", "查看用户列表"),
            new PermissionSeed(USER_CREATE, "用户-新增", "新增用户"),
            new PermissionSeed(USER_DELETE, "用户-删除", "删除用户"),
            new PermissionSeed(USER_STATUS, "用户-启停", "启用或停用用户"),
            new PermissionSeed(USER_PASSWORD, "用户-改密", "修改用户密码"),
            new PermissionSeed(USER_ASSIGN_ROLE, "用户-分配角色", "为用户分配角色"),
            new PermissionSeed(USER_GROUP_LIST, "用户分组-查看", "查看用户分组"),
            new PermissionSeed(USER_GROUP_CREATE, "用户分组-新增", "新增用户分组"),
            new PermissionSeed(USER_GROUP_UPDATE, "用户分组-修改", "修改用户分组"),
            new PermissionSeed(USER_GROUP_DELETE, "用户分组-删除", "删除用户分组"),
            new PermissionSeed(USER_GROUP_ASSIGN_USER, "用户分组-成员维护", "为用户分组分配成员"),
            new PermissionSeed(USER_GROUP_ASSIGN_DEVICE_GROUP, "用户分组-设备范围", "为用户分组分配设备分组范围"),
            new PermissionSeed(ROLE_LIST, "角色-查看", "查看角色列表"),
            new PermissionSeed(PERMISSION_LIST, "权限-查看", "查看权限列表"),
            new PermissionSeed(GROUP_LIST, "分组-查看", "查看设备分组"),
            new PermissionSeed(GROUP_CREATE, "分组-新增", "新增设备分组"),
            new PermissionSeed(GROUP_UPDATE, "分组-修改", "修改设备分组"),
            new PermissionSeed(GROUP_DELETE, "分组-删除", "删除设备分组"),
            new PermissionSeed(GROUP_DEVICE_LIST, "分组-设备查看", "查看分组设备"),
            new PermissionSeed(GROUP_DEVICE_ADD, "分组-设备添加", "添加设备到分组"),
            new PermissionSeed(GROUP_DEVICE_REMOVE, "分组-设备移除", "从分组移除设备"),
            new PermissionSeed(DEVICE_LIST, "设备-查看", "查看设备列表"),
            new PermissionSeed(DEVICE_ADB_LIST, "设备-ADB查看", "查看ADB设备列表"),
            new PermissionSeed(DEVICE_UPDATE, "设备-修改", "修改设备别名/备注"),
            new PermissionSeed(DEVICE_BIND, "设备-绑定", "绑定设备到分组"),
            new PermissionSeed(DEVICE_UNBIND, "设备-解绑", "从分组解绑设备"),
            new PermissionSeed(DEVICE_CONNECT, "设备-连接", "连接设备")
        );
    }

    public record PermissionSeed(String code, String name, String description) {}

    private DefaultPermissions() {}
}

package com.controlphonedesk.rbac.web;

import com.controlphonedesk.auth.RequirePermission;
import com.controlphonedesk.rbac.dto.UserGroupAssignDeviceGroupsRequest;
import com.controlphonedesk.rbac.dto.UserGroupAssignUsersRequest;
import com.controlphonedesk.rbac.dto.UserGroupDetail;
import com.controlphonedesk.rbac.dto.UserGroupInfo;
import com.controlphonedesk.rbac.dto.UserGroupUpsertRequest;
import com.controlphonedesk.rbac.seed.DefaultPermissions;
import com.controlphonedesk.rbac.service.UserGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-groups")
public class UserGroupController {
    private final UserGroupService userGroupService;

    public UserGroupController(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    @GetMapping
    @RequirePermission(DefaultPermissions.USER_GROUP_LIST)
    public List<UserGroupInfo> listGroups() {
        return userGroupService.listGroups();
    }

    @GetMapping("/{id}")
    @RequirePermission(DefaultPermissions.USER_GROUP_LIST)
    public ResponseEntity<UserGroupDetail> getGroup(@PathVariable Long id) {
        UserGroupDetail detail = userGroupService.getGroupDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    @PostMapping
    @RequirePermission(DefaultPermissions.USER_GROUP_CREATE)
    public ResponseEntity<UserGroupInfo> createGroup(@Valid @RequestBody UserGroupUpsertRequest request) {
        var group = userGroupService.createGroup(request.getName(), request.getRemark());
        UserGroupInfo info = new UserGroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setRemark(group.getRemark());
        info.setUserCount(group.getUsers().size());
        info.setDeviceGroupCount(group.getDeviceGroups().size());
        return ResponseEntity.status(HttpStatus.CREATED).body(info);
    }

    @PutMapping("/{id}")
    @RequirePermission(DefaultPermissions.USER_GROUP_UPDATE)
    public ResponseEntity<UserGroupInfo> updateGroup(@PathVariable Long id, @Valid @RequestBody UserGroupUpsertRequest request) {
        var group = userGroupService.updateGroup(id, request.getName(), request.getRemark());
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        UserGroupInfo info = new UserGroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setRemark(group.getRemark());
        info.setUserCount(group.getUsers().size());
        info.setDeviceGroupCount(group.getDeviceGroups().size());
        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(DefaultPermissions.USER_GROUP_DELETE)
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        userGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/users")
    @RequirePermission(DefaultPermissions.USER_GROUP_ASSIGN_USER)
    public ResponseEntity<UserGroupDetail> assignUsers(@PathVariable Long id, @RequestBody UserGroupAssignUsersRequest request) {
        var group = userGroupService.assignUsers(id, request.getUserIds());
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        UserGroupDetail detail = userGroupService.getGroupDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PutMapping("/{id}/device-groups")
    @RequirePermission(DefaultPermissions.USER_GROUP_ASSIGN_DEVICE_GROUP)
    public ResponseEntity<UserGroupDetail> assignDeviceGroups(
        @PathVariable Long id,
        @RequestBody UserGroupAssignDeviceGroupsRequest request
    ) {
        var group = userGroupService.assignDeviceGroups(id, request.getDeviceGroupIds());
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        UserGroupDetail detail = userGroupService.getGroupDetail(id);
        return ResponseEntity.ok(detail);
    }
}

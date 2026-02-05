package com.controlphonedesk.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class UserPrincipal {
    private final Long id;
    private final String username;
    private final boolean superAdmin;
    private final Set<String> permissions;

    public UserPrincipal(Long id, String username, boolean superAdmin, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.superAdmin = superAdmin;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }
}

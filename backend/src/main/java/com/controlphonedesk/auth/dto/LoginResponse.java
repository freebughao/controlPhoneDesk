package com.controlphonedesk.auth.dto;

import com.controlphonedesk.rbac.dto.UserInfo;
import java.time.Instant;

public class LoginResponse {
    private String token;
    private Instant expiresAt;
    private UserInfo user;

    public LoginResponse() {}

    public LoginResponse(String token, Instant expiresAt, UserInfo user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }
}

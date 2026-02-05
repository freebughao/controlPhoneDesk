package com.controlphonedesk.auth;

import com.controlphonedesk.AppProperties;
import com.controlphonedesk.auth.dto.LoginRequest;
import com.controlphonedesk.auth.dto.LoginResponse;
import com.controlphonedesk.rbac.dto.UserInfo;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserStatus;
import com.controlphonedesk.rbac.service.UserService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;

    public AuthController(
        UserService userService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AppProperties properties
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getUserByUsername(request.getUsername());
        if (user == null || user.getStatus() == UserStatus.DISABLED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.generateToken(user);
        Instant expiresAt = Instant.now().plus(properties.getSecurity().getJwt().getExpirationMinutes(), ChronoUnit.MINUTES);
        UserInfo info = userService.toUserInfo(user);
        return ResponseEntity.ok(new LoginResponse(token, expiresAt, info));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> me() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.getUserWithRoles(principal.getId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.toUserInfo(user));
    }
}

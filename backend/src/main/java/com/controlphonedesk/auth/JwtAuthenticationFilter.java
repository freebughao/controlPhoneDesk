package com.controlphonedesk.auth;

import com.controlphonedesk.rbac.entity.Permission;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserStatus;
import com.controlphonedesk.rbac.repo.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || path.startsWith("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            log.info("JWT missing: {} {}", request.getMethod(), request.getRequestURI());
            unauthorized(response, "Missing token");
            return;
        }
        log.info("JWT received: {} {} tokenPrefix={}", request.getMethod(), request.getRequestURI(), token.substring(0, Math.min(12, token.length())));
        User user;
        try {
            Claims claims = jwtService.parseToken(token);
            Object uidRaw = claims.get("uid");
            log.info("JWT claims uid raw type={} value={}", uidRaw == null ? "null" : uidRaw.getClass().getSimpleName(), uidRaw);
            Long userId = getUserId(claims);
            if (userId == null) {
                log.info("JWT invalid uid: {} {}", request.getMethod(), request.getRequestURI());
                unauthorized(response, "Invalid token");
                return;
            }
            user = userRepository.findWithRolesById(userId).orElse(null);
            if (user == null || user.getStatus() == UserStatus.DISABLED) {
                log.info("JWT user invalid: uid={} status={}", userId, user == null ? "null" : user.getStatus());
                unauthorized(response, "Invalid user");
                return;
            }
        } catch (Exception ex) {
            log.info("JWT parse failed: {} {} err={}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            unauthorized(response, "Invalid token");
            return;
        }

        log.info("JWT user ok: uid={} username={} superAdmin={}", user.getId(), user.getUsername(), user.isSuperAdmin());
        Set<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.isSuperAdmin(), permissions);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getRequestURI().startsWith("/ws/")) {
            return request.getParameter("token");
        }
        return null;
    }

    private Long getUserId(Claims claims) {
        Object uid = claims.get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        if (uid instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}

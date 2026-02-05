package com.controlphonedesk.auth;

import com.controlphonedesk.rbac.entity.Permission;
import com.controlphonedesk.rbac.entity.User;
import com.controlphonedesk.rbac.entity.UserStatus;
import com.controlphonedesk.rbac.repo.UserRepository;
import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        String token = resolveToken(request);
        if (token == null) {
            log.info("WS JWT missing: {}", request.getURI());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        log.info("WS JWT received: {} tokenPrefix={}", request.getURI(), token.substring(0, Math.min(12, token.length())));
        try {
            Claims claims = jwtService.parseToken(token);
            Object uidRaw = claims.get("uid");
            log.info("WS JWT claims uid raw type={} value={}", uidRaw == null ? "null" : uidRaw.getClass().getSimpleName(), uidRaw);
            Long userId = getUserId(claims);
            if (userId == null) {
                log.info("WS JWT invalid uid: {}", request.getURI());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            User user = userRepository.findWithRolesById(userId).orElse(null);
            if (user == null || user.getStatus() == UserStatus.DISABLED) {
                log.info("WS JWT user invalid: uid={} status={}", userId, user == null ? "null" : user.getStatus());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
            log.info("WS JWT user ok: uid={} username={} superAdmin={}", user.getId(), user.getUsername(), user.isSuperAdmin());
            attributes.put("uid", userId);
            attributes.put("superAdmin", user.isSuperAdmin());
            attributes.put("permissions", permissions);
            return true;
        } catch (Exception ex) {
            log.info("WS JWT parse failed: {} err={}", request.getURI(), ex.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}

    private String resolveToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        String query = request.getURI().getQuery();
        if (query == null) {
            return null;
        }
        return UriComponentsBuilder.newInstance().query(query).build().getQueryParams().getFirst("token");
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
}

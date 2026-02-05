package com.controlphonedesk.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission permission = findPermission(handlerMethod);
        if (permission == null) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return deny(response, "Unauthorized");
        }
        if (principal.isSuperAdmin()) {
            return true;
        }
        Set<String> permissions = principal.getPermissions();
        if (permissions != null && permissions.contains(permission.value())) {
            return true;
        }
        return deny(response, "Forbidden");
    }

    private RequirePermission findPermission(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        if (permission != null) {
            return permission;
        }
        return handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
    }

    private boolean deny(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
        return false;
    }
}

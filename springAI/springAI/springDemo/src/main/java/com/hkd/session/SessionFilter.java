package com.hkd.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * 服务端会话过滤器。
 *
 * <p>浏览器第一次访问时，由服务端签发一个随机会话 ID、写进 HttpOnly Cookie，
 * 之后的请求都从 Cookie 里读回它。会话 ID 就是这里的“身份”：</p>
 * <ul>
 *   <li>放在 HttpOnly Cookie 里，前端 JS 读不到、也伪造不了；</li>
 *   <li>不再由前端自己拼，堵住“猜 / 改别人的 ID 就能越权”的口子。</li>
 * </ul>
 */
@Component
public class SessionFilter extends OncePerRequestFilter {

    /** 会话 Cookie 名 */
    public static final String COOKIE_NAME = "dcar_sid";

    private static final Duration MAX_AGE = Duration.ofDays(30);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sessionId = readSessionId(request);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(request, sessionId).toString());
        }
        SessionContext.set(sessionId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
    }

    private String readSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(HttpServletRequest request, String sessionId) {
        String path = request.getContextPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        return ResponseCookie.from(COOKIE_NAME, sessionId)
                .httpOnly(true)
                .path(path)
                .maxAge(MAX_AGE)
                .sameSite("Lax")
                .build();
    }
}
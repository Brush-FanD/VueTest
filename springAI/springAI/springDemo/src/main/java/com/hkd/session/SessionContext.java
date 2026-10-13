package com.hkd.session;

/**
 * 当前请求的会话 ID 持有者。
 *
 * <p>由 {@link SessionFilter} 在请求进入时写入、请求结束时清理。
 * 业务层（AI 记忆 / 视频 / 评论）通过它拿到“当前是谁”，
 * 从而实现「服务端签发会话 + 谁的数据谁能改」。</p>
 */
public final class SessionContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private SessionContext() {
    }

    /** 当前请求的会话 ID，未初始化时为 null */
    public static String get() {
        return HOLDER.get();
    }

    /** 当前请求的会话 ID，未初始化时给一个匿名兜底值 */
    public static String require() {
        String sid = HOLDER.get();
        return sid == null ? "anonymous" : sid;
    }

    static void set(String sessionId) {
        HOLDER.set(sessionId);
    }

    static void clear() {
        HOLDER.remove();
    }
}
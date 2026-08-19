package com.example.appointment.aspect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts secrets from strings before they are written to any log sink.
 *
 * The existing LoggingAspect logs method arguments and return values via
 * their toString() (Lombok @Data-generated, e.g. "LoginRequest(username=bob,
 * password=hunter2)"). Without this, plaintext passwords and JWTs land in
 * stdout on every login/register call. This class is applied at the
 * aspect level so it protects every controller/service automatically,
 * without needing per-DTO changes.
 */
public final class LogSanitizer {

    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(password|passwd|pwd|token|secret|apiKey|api_key)\\s*=\\s*([^,)\\]}]+)"
    );

    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
    );

    private LogSanitizer() {
    }

    public static String sanitize(Object value) {
        if (value == null) {
            return "null";
        }
        return sanitize(String.valueOf(value));
    }

    public static String sanitize(String text) {
        if (text == null) {
            return null;
        }
        Matcher jwtMatcher = JWT.matcher(text);
        String result = jwtMatcher.replaceAll("<REDACTED_JWT>");

        Matcher kvMatcher = KEY_VALUE_SECRET.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (kvMatcher.find()) {
            kvMatcher.appendReplacement(sb, Matcher.quoteReplacement(kvMatcher.group(1) + "=<REDACTED>"));
        }
        kvMatcher.appendTail(sb);
        return sb.toString();
    }
}

package com.example.appointment.aspect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Forwards structured log lines to the AI log classification service.
 *
 * Intentionally decoupled from the request thread, and deliberately built
 * on the JDK's built-in java.net.http.HttpClient rather than adding a new
 * dependency (e.g. WebClient/reactor) just for this - keeps the change
 * minimal and avoids mixing servlet + reactive stacks in an already
 * complex Spring MVC + WebSocket + Security configuration.
 *
 *   - events are pushed onto a small bounded in-memory queue (never blocks
 *     the caller; if the queue is full, the event is silently dropped)
 *   - a single background worker thread drains the queue and fires the
 *     HTTP call asynchronously with a short timeout
 *   - any failure (AI service down, timeout, DNS failure, etc.) is caught
 *     and logged at debug level only - it can NEVER propagate back into
 *     application code or affect appointment booking.
 *
 * This satisfies the "AI service must be auxiliary, never a hard
 * dependency" requirement: if ai-log-classifier is unreachable, the
 * appointment application behaves exactly as if this class did not exist.
 */
@Component
public class AiLogForwarder {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiLogForwarder.class);

    private final boolean enabled;
    private final String url;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(1000);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-log-forwarder");
        t.setDaemon(true);
        return t;
    });

    public AiLogForwarder(
            @Value("${ai.log-classifier.enabled:false}") boolean enabled,
            @Value("${ai.log-classifier.url:http://ai-log-classifier:8000/api/v1/classify}") String url,
            @Value("${ai.log-classifier.timeout-ms:800}") long timeoutMs
    ) {
        this.enabled = enabled;
        this.url = url;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 500)))
                .build();

        if (enabled) {
            worker.submit(this::drainLoop);
        }
    }

    /**
     * Non-blocking: enqueues the (already sanitized) log line and returns
     * immediately. Drops the event if the queue is full rather than
     * blocking or growing unbounded.
     */
    public void submit(String sanitizedLogLine) {
        if (!enabled || sanitizedLogLine == null || sanitizedLogLine.isBlank()) {
            return;
        }
        if (!queue.offer(sanitizedLogLine)) {
            log.debug("AI log forwarder queue full, dropping event");
        }
    }

    private void drainLoop() {
        while (true) {
            try {
                String line = queue.take();
                send(line);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.debug("AI log forwarder error, dropping event: {}", e.getMessage());
            }
        }
    }

    private void send(String line) {
        try {
            String jsonBody = "{\"log\":\"" + escapeJson(line) + "\",\"service\":\"appointment-service\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Fire-and-forget: sendAsync() returns immediately, and any
            // failure is only ever observed inside this callback - never
            // thrown back into the caller.
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        log.debug("AI log forwarder request failed: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.debug("AI log forwarder failed to build request: {}", e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}

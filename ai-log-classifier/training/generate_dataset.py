"""
Generates the synthetic training dataset (training/dataset.csv).

IMPORTANT: This dataset is entirely synthetic. It was written by hand /
templated to resemble the log shapes this specific application (a Spring
Boot appointment booking system with JWT auth, MySQL, and
appointment/service/schedule domain objects) actually produces, based on
inspection of its controllers, services, and existing LoggingAspect
output. It contains NO real user data, no real credentials, and no real
production log lines.

It is intentionally over 3000 rows so the model has enough examples per
class to generalize past exact string memorization. Replace this file
with real (sanitized) production logs once enough have accumulated -
the training pipeline does not care where dataset.csv came from as long
as it keeps the `log,label` schema.
"""

import csv
import random

random.seed(42)

LABELS = [
    "INFO",
    "WARNING",
    "APPLICATION_ERROR",
    "DATABASE_ERROR",
    "AUTHENTICATION_ERROR",
    "AUTHORIZATION_ERROR",
    "VALIDATION_ERROR",
    "NETWORK_ERROR",
    "PERFORMANCE_WARNING",
    "SECURITY_ALERT",
    "SYSTEM_ERROR",
    "UNKNOWN",
]

USERS = ["12345", "8891", "203", "77410", "555", "9002", "31", "4471", "60021"]
SERVICE_IDS = ["svc-101", "svc-202", "svc-9", "svc-77", "svc-330"]
EMAILS = ["user@example.com", "customer1@mail.com", "staff2@clinic.local", "admin@system.local"]
IPS = ["192.168.1.10", "10.0.0.5", "172.17.0.3", "203.0.113.42", "198.51.100.7"]
ENDPOINTS = [
    "/api/appointments",
    "/api/appointments/{id}/cancel",
    "/api/appointments/{id}/status",
    "/api/services",
    "/api/services/{id}/available-slots",
    "/api/auth/login",
    "/api/auth/register",
    "/api/admin/schedules",
]
DURATIONS_SLOW = [1500, 2200, 3100, 4200, 5000, 6100, 7300]
DURATIONS_OK = [45, 80, 120, 200, 310]

TEMPLATES = {
    "INFO": [
        "User {user} successfully logged in",
        "User {user} registered a new account",
        "Appointment created successfully for user {user} with service {service}",
        "Appointment {id} status updated to ACCEPTED",
        "Appointment {id} cancelled by user {user}",
        "Service {service} retrieved successfully",
        "Available slots fetched for service {service} on requested date",
        "User {user} logged out",
        "Working schedule created for staff member {user}",
        "Reminder email sent to {email} for upcoming appointment",
        "Method: getMyAppointments | Arguments: []",
        "Method Completed: createAppointment | Result: Appointment{{id={id}, status=PENDING}}",
        "Health check passed for appointment-service",
        "Statistics report generated for admin dashboard",
        "WebSocket connection established for user {user}",
    ],
    "WARNING": [
        "Appointment {id} requested for a slot close to closing time",
        "User {user} attempted to book an already-cancelled appointment",
        "Deprecated API version used by client for endpoint {endpoint}",
        "Retrying email delivery to {email} after transient failure",
        "Working schedule overlaps detected for staff member {user}, auto-resolved",
        "Cache miss for service {service} listing, falling back to database",
        "Slow query warning: appointment lookup took longer than expected",
        "Configuration property spring.jpa.show-sql is enabled in a non-dev profile",
    ],
    "APPLICATION_ERROR": [
        "Exception in method: createAppointment | Message: Requested slot is no longer available",
        "Exception in method: cancelAppointment | Message: Appointment {id} not found",
        "Exception in method: updateStatus | Message: Invalid status transition from FINISHED to PENDING",
        "NullPointerException while processing appointment request for user {user}",
        "Failed to serialize response for endpoint {endpoint}",
        "Unhandled RuntimeException in AppointmentService.getAvailableSlots",
        "Exception in method: register | Message: Unexpected error creating user account",
        "IllegalStateException: working schedule not initialized for staff {user}",
    ],
    "DATABASE_ERROR": [
        "Failed to connect to PostgreSQL database",
        "Connection refused while connecting to MySQL at mysql:3306",
        "com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure",
        "Deadlock found when trying to get lock on table appointments; try restarting transaction",
        "org.hibernate.exception.ConstraintViolationException: could not execute statement",
        "Connection pool exhausted, unable to obtain JDBC connection for appointmentdb",
        "Query timeout while fetching appointments for user {user}",
        "Data truncation: Data too long for column 'notes' at row 1",
        "Table 'appointmentdb.working_schedule' doesn't exist",
        "Failed to acquire JDBC Connection: HikariPool-1 connection is not available",
    ],
    "AUTHENTICATION_ERROR": [
        "Invalid JWT token",
        "JWT signature does not match locally computed signature",
        "Expired JWT token for user {user}",
        "Login failed for user {email}: invalid credentials",
        "Malformed Authorization header on request to {endpoint}",
        "JWT token missing on protected endpoint {endpoint}",
        "AuthenticationException: Bad credentials for user {email}",
        "Failed to parse JWT claims for request to {endpoint}",
    ],
    "AUTHORIZATION_ERROR": [
        "User {user} does not have permission to access {endpoint}",
        "AccessDeniedException: role CUSTOMER cannot perform this action on {endpoint}",
        "User {user} attempted to access admin-only endpoint {endpoint}",
        "Forbidden: STAFF role required to update appointment status",
        "User {user} tried to cancel an appointment belonging to another user",
        "403 Forbidden returned for request to {endpoint} by user {user}",
    ],
    "VALIDATION_ERROR": [
        "Validation failed: startTime must not be null",
        "Validation failed: email must be a well-formed email address",
        "Validation failed: password must be at least 8 characters",
        "Invalid request body for endpoint {endpoint}: missing required field 'serviceId'",
        "MethodArgumentNotValidException: appointment date must be in the future",
        "Validation failed: username already exists",
        "Rejected request: appointment duration must be positive",
    ],
    "NETWORK_ERROR": [
        "Connection timed out while calling SMTP server smtp.gmail.com:587",
        "SocketTimeoutException while sending reminder email to {email}",
        "Unable to reach downstream service from {ip}",
        "Read timed out on connection to mysql:3306",
        "Proxy error: unable to forward request from Apache to spring-app:8443",
        "SSL handshake failed while connecting to mail server",
        "Connection reset by peer while streaming websocket data for user {user}",
    ],
    "PERFORMANCE_WARNING": [
        "Appointment API request took {duration} ms",
        "Slow response detected for {endpoint}: {duration} ms",
        "Database query for available slots took {duration} ms",
        "Method getAllAppointments took {duration} ms to complete",
        "High memory usage detected in appointment-service JVM heap",
        "GC pause of {duration} ms detected on spring-app container",
        "Thread pool queue depth elevated while processing {endpoint} requests",
    ],
    "SECURITY_ALERT": [
        "Multiple failed login attempts detected from the same IP {ip}",
        "Possible brute force attack detected against /api/auth/login from {ip}",
        "SQL injection pattern detected in request parameter for {endpoint}",
        "Repeated 401 Unauthorized responses from IP {ip} within short window",
        "Suspicious request payload containing script tags blocked for {endpoint}",
        "Account for user {user} locked after repeated failed authentication attempts",
        "Unusual number of admin actions performed by user {user} in a short period",
        "Request from {ip} attempted path traversal on static resource path",
    ],
    "SYSTEM_ERROR": [
        "OutOfMemoryError: Java heap space in appointment-service container (attempt {id})",
        "Container spring-app restarted unexpectedly at cycle {id}",
        "Disk space critically low on mysql-db volume ({duration} MB free)",
        "Application failed to start: port 8443 already in use (pid {id})",
        "Fatal error: unable to load SSL keystore, service unavailable (code {id})",
        "Docker health check failed for spring-app container, retry {id}",
        "StackOverflowError encountered while processing recursive schedule computation for staff {user}",
        "JVM garbage collector could not reclaim memory, heap dump {id} written",
    ],
    "UNKNOWN": [
        "Completely unrelated unknown event (ref {id})",
        "xkcd-node-{id} heartbeat received on unmapped channel",
        "Legacy subsystem emitted an unrecognized diagnostic code {id}",
        "Miscellaneous debug marker {id} left in codebase",
        "Unclassified telemetry ping from unknown source {ip}",
        "Placeholder log entry {id} pending removal",
        "Uninterpreted binary blob logged at offset {id}",
        "No handler registered for event type {id}",
    ],
}


def _fill(template: str) -> str:
    return template.format(
        user=random.choice(USERS),
        service=random.choice(SERVICE_IDS),
        id=random.choice(USERS),
        email=random.choice(EMAILS),
        ip=random.choice(IPS),
        endpoint=random.choice(ENDPOINTS),
        duration=random.choice(DURATIONS_SLOW if random.random() < 0.7 else DURATIONS_OK),
    )


def generate(rows_per_label: int = 260) -> list:
    data = []
    for label, templates in TEMPLATES.items():
        seen = set()
        attempts = 0
        # oversample by re-filling templates with different random values
        # until we hit the target row count per label (with light dedup)
        while len(seen) < rows_per_label and attempts < rows_per_label * 20:
            attempts += 1
            template = random.choice(templates)
            line = _fill(template)
            if line not in seen:
                seen.add(line)
        unique_lines = list(seen)
        # Some categories (e.g. UNKNOWN, SYSTEM_ERROR) have less lexical
        # diversity in their hand-written templates than others. Rather than
        # silently shipping a smaller class, pad up to the target count by
        # resampling - documented as a known limitation of the synthetic
        # dataset in the README, to be replaced by real logs over time.
        label_rows = list(unique_lines)
        while len(label_rows) < rows_per_label:
            label_rows.append(random.choice(unique_lines))
        for line in label_rows[:rows_per_label]:
            data.append((line, label))
    random.shuffle(data)
    return data


def main():
    rows = generate(rows_per_label=260)
    out_path = "training/dataset.csv"
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["log", "label"])
        writer.writerows(rows)
    print(f"Wrote {len(rows)} rows to {out_path}")
    counts = {}
    for _, label in rows:
        counts[label] = counts.get(label, 0) + 1
    for label in LABELS:
        print(f"  {label}: {counts.get(label, 0)}")


if __name__ == "__main__":
    main()

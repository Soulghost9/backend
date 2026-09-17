package com.sdrp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * SMART DISASTER RESPONSE PLATFORM (SDRP) - CONSOLIDATED BACKEND
 * Production-Grade Single-File Backend Specification
 */
public class SDRPBackendComplete {

    private static final Logger logger = LoggerFactory.getLogger(SDRPBackendComplete.class);

    public static void main(String[] args) {
        logger.info("=================================================");
        logger.info("  SMART DISASTER RESPONSE PLATFORM (SDRP)");
        logger.info("=================================================");

        boolean demoMode = false;
        int port = 8085;

        for (int i = 0; i < args.length; i++) {
            if ("--demo".equalsIgnoreCase(args[i])) {
                demoMode = true;
            } else if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }

        if (demoMode) {
            runEndToEndDemo();
        } else {
            try {
                runEndToEndDemo();
                SdrpHttpServer server = new SdrpHttpServer(port);
                server.start();
            } catch (Exception e) {
                logger.error("Failed to start SDRP Backend Server", e);
            }
        }
    }

    public static void runEndToEndDemo() {
        System.out.println("\n--- RUNNING SDRP END-TO-END VERIFICATION DEMO ---");
        try {
            AlertService alertService = new AlertService();
            alertService.addListener(alert -> System.out.println("  >>> [NOTIFICATION BROADCAST] Priority: " + alert.getPriority() + " | Type: " + alert.getType() + " | Message: " + alert.getMessage()));

            AuthenticationService authService = new AuthenticationService();
            SOSService sosService = new SOSService(alertService);
            DisasterReportService disasterService = new DisasterReportService(alertService);

            // 1. Register Users
            System.out.println("\n1. Testing User Registration:");
            User responder = authService.register("demoresponder", "responder@sdrp.org", "Pass123456", "RESPONDER", 12.9716, 77.5946);
            System.out.println("  Registered Responder: " + responder);

            User victim = authService.register("demovictim", "victim@sdrp.org", "Pass123456", "VICTIM", 12.9800, 77.6000);
            System.out.println("  Registered Victim: " + victim);

            // 2. User Login
            System.out.println("\n2. Testing User Login:");
            User loggedInUser = authService.login("demovictim", "Pass123456");
            System.out.println("  Logged in user successfully: " + loggedInUser.getUsername());

            // 3. Create SOS Alert
            System.out.println("\n3. Testing SOS Emergency Creation:");
            SOSAlert sos = sosService.createSOSAlert(victim.getUserId(), 12.9800, 77.6000, "CRITICAL", "Trapped in flood waters");
            System.out.println("  Created SOS Alert: " + sos.getSosId() + " [Status: " + sos.getStatus() + "]");

            // 4. Query Active SOS Alerts
            System.out.println("\n4. Testing Active SOS Queries:");
            List<SOSAlert> activeAlerts = sosService.getActiveSOSAlerts();
            System.out.println("  Total Active SOS Alerts: " + activeAlerts.size());

            // 5. Acknowledge SOS
            System.out.println("\n5. Testing Responder SOS Acknowledgment:");
            SOSAlert ackSos = sosService.acknowledgeSOSAlert(sos.getSosId(), responder.getUserId());
            System.out.println("  SOS Acknowledged! New Status: " + ackSos.getStatus() + " | Responders Count: " + ackSos.getRespondersCount());

            // 6. Submit Disaster Report
            System.out.println("\n6. Testing Disaster Report Submission:");
            DisasterReport report = disasterService.submitDisasterReport(victim.getUserId(), "FLOOD", 12.9800, 77.6000, "HIGH", "Severe flooding in residential sector", 45);
            System.out.println("  Report Submitted: " + report.getReportId() + " [Severity: " + report.getSeverity() + "]");

            // 7. Verify Disaster Report
            System.out.println("\n7. Testing Admin Verification:");
            DisasterReport verified = disasterService.verifyDisasterReport(report.getReportId(), responder.getUserId());
            System.out.println("  Report Verified! New Status: " + verified.getStatus());

            System.out.println("\n=================================================");
            System.out.println("   SDRP BACKEND ALL DEMO VERIFICATION CHECKS PASSED!");
            System.out.println("=================================================\n");

        } catch (Exception e) {
            System.err.println("Demo execution failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // EXCEPTION HIERARCHY
    // =========================================================================
    public static class SDRPException extends Exception {
        private final String errorCode;
        public SDRPException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }

    public static class AuthenticationException extends SDRPException {
        public AuthenticationException(String message) { super(message, "AUTH_FAILED"); }
    }

    public static class ValidationException extends SDRPException {
        public ValidationException(String message) { super(message, "VALIDATION_FAILED"); }
    }

    public static class DatabaseException extends SDRPException {
        public DatabaseException(String message) { super(message, "DB_ERROR"); }
    }

    // =========================================================================
    // DOMAIN MODELS
    // =========================================================================
    public static class User {
        private String userId;
        private String username;
        private String email;
        private String passwordHash;
        private String userType;
        private double latitude;
        private double longitude;
        private LocalDateTime createdAt;
        private boolean isActive;

        public User() {}
        public User(String userId, String username, String email, String userType, double latitude, double longitude) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.userType = userType;
            this.latitude = latitude;
            this.longitude = longitude;
            this.createdAt = LocalDateTime.now();
            this.isActive = true;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        @Override
        public String toString() {
            return "User{" + "userId='" + userId + '\'' + ", username='" + username + '\'' + ", userType='" + userType + '\'' + '}';
        }
    }

    public static class SOSAlert {
        private String sosId;
        private String userId;
        private double latitude;
        private double longitude;
        private String urgencyLevel;
        private String description;
        private LocalDateTime createdAt;
        private String status;
        private int respondersCount;

        public SOSAlert() {}
        public SOSAlert(String sosId, String userId, double latitude, double longitude, String urgencyLevel, String description) {
            this.sosId = sosId;
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.urgencyLevel = urgencyLevel;
            this.description = description;
            this.createdAt = LocalDateTime.now();
            this.status = "ACTIVE";
            this.respondersCount = 0;
        }

        public String getSosId() { return sosId; }
        public void setSosId(String sosId) { this.sosId = sosId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getUrgencyLevel() { return urgencyLevel; }
        public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getRespondersCount() { return respondersCount; }
        public void setRespondersCount(int respondersCount) { this.respondersCount = respondersCount; }
    }

    public static class DisasterReport {
        private String reportId;
        private String userId;
        private String disasterType;
        private double latitude;
        private double longitude;
        private String severity;
        private String description;
        private LocalDateTime reportedAt;
        private String status;
        private int affectedPeople;
        private String verifiedBy;

        public DisasterReport() {}
        public DisasterReport(String reportId, String userId, String disasterType, double latitude, double longitude, String severity, String description, int affectedPeople) {
            this.reportId = reportId;
            this.userId = userId;
            this.disasterType = disasterType;
            this.latitude = latitude;
            this.longitude = longitude;
            this.severity = severity;
            this.description = description;
            this.affectedPeople = affectedPeople;
            this.reportedAt = LocalDateTime.now();
            this.status = "PENDING";
        }

        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDisasterType() { return disasterType; }
        public void setDisasterType(String disasterType) { this.disasterType = disasterType; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getReportedAt() { return reportedAt; }
        public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getAffectedPeople() { return affectedPeople; }
        public void setAffectedPeople(int affectedPeople) { this.affectedPeople = affectedPeople; }
        public String getVerifiedBy() { return verifiedBy; }
        public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    }

    public static class Alert {
        private String alertId;
        private String type;
        private String message;
        private String priority;
        private LocalDateTime createdAt;
        private boolean isAcknowledged;

        public Alert() {}
        public Alert(String alertId, String type, String message, String priority) {
            this.alertId = alertId;
            this.type = type;
            this.message = message;
            this.priority = priority;
            this.createdAt = LocalDateTime.now();
            this.isAcknowledged = false;
        }

        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public boolean isAcknowledged() { return isAcknowledged; }
        public void setAcknowledged(boolean acknowledged) { isAcknowledged = acknowledged; }
    }

    // =========================================================================
    // INPUT VALIDATOR
    // =========================================================================
    public static class InputValidator {
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

        public static void validateUser(String username, String email, String password) throws ValidationException {
            if (username == null || username.trim().isEmpty()) throw new ValidationException("Username cannot be empty");
            if (!USERNAME_PATTERN.matcher(username).matches()) throw new ValidationException("Username must be 3-20 alphanumeric characters");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) throw new ValidationException("Invalid email format");
            if (password == null || password.length() < 8) throw new ValidationException("Password must be at least 8 characters");
        }

        public static void validateLocation(double lat, double lon) throws ValidationException {
            if (lat < -90.0 || lat > 90.0) throw new ValidationException("Latitude must be between -90 and 90");
            if (lon < -180.0 || lon > 180.0) throw new ValidationException("Longitude must be between -180 and 180");
        }

        public static void validateSOSAlert(String userId, double lat, double lon, String urgency, String description) throws ValidationException {
            if (userId == null || userId.trim().isEmpty()) throw new ValidationException("User ID is required");
            validateLocation(lat, lon);
            if (urgency == null || !urgency.matches("CRITICAL|HIGH|MEDIUM|LOW")) throw new ValidationException("Invalid urgency level: " + urgency);
            if (description == null || description.trim().isEmpty()) throw new ValidationException("Description cannot be empty");
        }

        public static void validateDisasterReport(String disasterType, String severity, String description, int affectedPeople) throws ValidationException {
            if (disasterType == null || !disasterType.matches("EARTHQUAKE|FLOOD|FIRE|LANDSLIDE|STORM|TSUNAMI|OTHER")) throw new ValidationException("Invalid disaster type: " + disasterType);
            if (severity == null || !severity.matches("LOW|MEDIUM|HIGH|CRITICAL")) throw new ValidationException("Invalid severity level: " + severity);
            if (description == null || description.trim().isEmpty()) throw new ValidationException("Description cannot be empty");
            if (affectedPeople < 0) throw new ValidationException("Affected people count cannot be negative");
        }
    }

    // =========================================================================
    // DATABASE MANAGER
    // =========================================================================
    public static class DatabaseManager {
        private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
        private static final String DB_URL = "jdbc:h2:mem:sdrp_db;DB_CLOSE_DELAY=-1";
        private static final String DB_USER = "sa";
        private static final String DB_PASSWORD = "";
        private static DatabaseManager instance;

        static {
            try { Class.forName("org.h2.Driver"); } catch (ClassNotFoundException e) { log.error("H2 driver error", e); }
        }

        private DatabaseManager() { initSchema(); }

        public static synchronized DatabaseManager getInstance() {
            if (instance == null) instance = new DatabaseManager();
            return instance;
        }

        public Connection getConnection() throws DatabaseException {
            try { return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); }
            catch (SQLException e) { throw new DatabaseException("Database connection error: " + e.getMessage()); }
        }

        public void initSchema() {
            try (Connection conn = getConnection()) {
                InputStream is = getClass().getClassLoader().getResourceAsStream("schema.h2.sql");
                if (is != null) {
                    org.h2.tools.RunScript.execute(conn, new InputStreamReader(is));
                    log.info("Database schema initialized.");
                }
            } catch (Exception e) {
                log.error("Schema init error", e);
            }
        }
    }

    // =========================================================================
    // CAFFEINE CACHE MANAGER
    // =========================================================================
    public static class CaffeineCacheManager {
        private final Cache<String, User> userCache;
        private final Cache<String, SOSAlert> sosAlertCache;
        private final Cache<String, List<SOSAlert>> activeSOSListCache;
        private final Cache<String, List<DisasterReport>> disasterReportsCache;
        private static CaffeineCacheManager instance;

        private CaffeineCacheManager() {
            userCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build();
            sosAlertCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).maximumSize(1000).build();
            activeSOSListCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).maximumSize(100).build();
            disasterReportsCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(100).build();
        }

        public static synchronized CaffeineCacheManager getInstance() {
            if (instance == null) instance = new CaffeineCacheManager();
            return instance;
        }

        public Cache<String, User> getUserCache() { return userCache; }
        public Cache<String, SOSAlert> getSosAlertCache() { return sosAlertCache; }
        public Cache<String, List<SOSAlert>> getActiveSOSListCache() { return activeSOSListCache; }
        public Cache<String, List<DisasterReport>> getDisasterReportsCache() { return disasterReportsCache; }
        public void invalidateActiveSOS() { activeSOSListCache.invalidateAll(); }
        public void invalidateDisasterReports() { disasterReportsCache.invalidateAll(); }
    }

    // =========================================================================
    // SERVICES
    // =========================================================================
    public static class AlertService {
        private static final Logger log = LoggerFactory.getLogger(AlertService.class);
        private final DatabaseManager dbManager = DatabaseManager.getInstance();
        private final List<AlertListener> listeners = new ArrayList<>();

        public interface AlertListener { void onAlertBroadcasted(Alert alert); }
        public void addListener(AlertListener listener) { listeners.add(listener); }

        public void broadcastAlert(Alert alert) throws DatabaseException {
            String sql = "INSERT INTO alerts (alert_id, type, message, priority, created_at, is_acknowledged) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, alert.getAlertId());
                pstmt.setString(2, alert.getType());
                pstmt.setString(3, alert.getMessage());
                pstmt.setString(4, alert.getPriority());
                pstmt.setTimestamp(5, Timestamp.valueOf(alert.getCreatedAt()));
                pstmt.setBoolean(6, alert.isAcknowledged());
                pstmt.executeUpdate();

                for (AlertListener listener : listeners) {
                    try { listener.onAlertBroadcasted(alert); } catch (Exception e) { log.error("Listener error", e); }
                }
                log.info("Alert broadcasted: {} [{}]", alert.getAlertId(), alert.getPriority());
            } catch (SQLException e) { throw new DatabaseException("Alert broadcast error: " + e.getMessage()); }
        }

        public List<Alert> getAlertsByPriority(String priority) throws DatabaseException {
            List<Alert> alerts = new ArrayList<>();
            String sql = "SELECT * FROM alerts WHERE priority = ? ORDER BY created_at DESC";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, priority);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Alert alert = new Alert(rs.getString("alert_id"), rs.getString("type"), rs.getString("message"), rs.getString("priority"));
                        alert.setAcknowledged(rs.getBoolean("is_acknowledged"));
                        alerts.add(alert);
                    }
                }
                return alerts;
            } catch (SQLException e) { throw new DatabaseException("Query error: " + e.getMessage()); }
        }
    }

    public static class AuthenticationService {
        private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
        private final DatabaseManager dbManager = DatabaseManager.getInstance();
        private final CaffeineCacheManager cacheManager = CaffeineCacheManager.getInstance();

        public String hashPassword(String password) throws AuthenticationException {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) sb.append(String.format("%02x", b));
                return sb.toString();
            } catch (NoSuchAlgorithmException e) { throw new AuthenticationException("SHA-256 unavailable"); }
        }

        public User register(String username, String email, String password, String userType, double lat, double lon)
                throws ValidationException, AuthenticationException, DatabaseException {
            InputValidator.validateUser(username, email, password);
            InputValidator.validateLocation(lat, lon);
            if (!userType.matches("RESPONDER|VICTIM|ADMIN")) throw new ValidationException("Invalid user type: " + userType);

            String checkSql = "SELECT user_id FROM users WHERE username = ? OR email = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) throw new ValidationException("Username/email already exists");
                }
            } catch (SQLException e) { throw new DatabaseException("DB error: " + e.getMessage()); }

            String userId = UUID.randomUUID().toString();
            String hash = hashPassword(password);
            String insertSql = "INSERT INTO users (user_id, username, email, password_hash, user_type, latitude, longitude, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, username);
                pstmt.setString(3, email);
                pstmt.setString(4, hash);
                pstmt.setString(5, userType);
                pstmt.setDouble(6, lat);
                pstmt.setDouble(7, lon);
                pstmt.setBoolean(8, true);
                pstmt.executeUpdate();

                User user = new User(userId, username, email, userType, lat, lon);
                user.setPasswordHash(hash);
                cacheManager.getUserCache().put(userId, user);
                log.info("Registered user: {}", username);
                return user;
            } catch (SQLException e) { throw new DatabaseException("Registration error: " + e.getMessage()); }
        }

        public User login(String username, String password) throws AuthenticationException, ValidationException, DatabaseException {
            if (username == null || password == null) throw new ValidationException("Username and password required");
            String sql = "SELECT * FROM users WHERE username = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) throw new AuthenticationException("Invalid credentials");
                    String stored = rs.getString("password_hash");
                    if (!stored.equals(hashPassword(password))) throw new AuthenticationException("Invalid credentials");

                    User user = new User(rs.getString("user_id"), rs.getString("username"), rs.getString("email"), rs.getString("user_type"), rs.getDouble("latitude"), rs.getDouble("longitude"));
                    user.setPasswordHash(stored);
                    user.setActive(rs.getBoolean("is_active"));
                    cacheManager.getUserCache().put(user.getUserId(), user);
                    log.info("Logged in: {}", username);
                    return user;
                }
            } catch (SQLException e) { throw new DatabaseException("Auth DB error: " + e.getMessage()); }
        }
    }

    public static class SOSService {
        private static final Logger log = LoggerFactory.getLogger(SOSService.class);
        private final DatabaseManager dbManager = DatabaseManager.getInstance();
        private final CaffeineCacheManager cacheManager = CaffeineCacheManager.getInstance();
        private final AlertService alertService;

        public SOSService(AlertService alertService) { this.alertService = alertService; }

        public SOSAlert createSOSAlert(String userId, double lat, double lon, String urgency, String description) throws ValidationException, DatabaseException {
            InputValidator.validateSOSAlert(userId, lat, lon, urgency, description);
            String sosId = UUID.randomUUID().toString();
            SOSAlert sos = new SOSAlert(sosId, userId, lat, lon, urgency, description);

            String sql = "INSERT INTO sos_alerts (sos_id, user_id, latitude, longitude, urgency_level, description, status, responders_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sosId);
                pstmt.setString(2, userId);
                pstmt.setDouble(3, lat);
                pstmt.setDouble(4, lon);
                pstmt.setString(5, urgency);
                pstmt.setString(6, description);
                pstmt.setString(7, "ACTIVE");
                pstmt.setInt(8, 0);
                pstmt.executeUpdate();

                alertService.broadcastAlert(new Alert(UUID.randomUUID().toString(), "SOS_RESPONSE", "Emergency SOS [" + urgency + "] at (" + lat + ", " + lon + "): " + description, urgency));
                cacheManager.getSosAlertCache().put(sosId, sos);
                cacheManager.invalidateActiveSOS();
                log.info("SOS Created: {}", sosId);
                return sos;
            } catch (SQLException e) { throw new DatabaseException("SOS error: " + e.getMessage()); }
        }

        public SOSAlert acknowledgeSOSAlert(String sosId, String responderId) throws DatabaseException {
            String updateSql = "UPDATE sos_alerts SET status = 'ACKNOWLEDGED', responders_count = responders_count + 1 WHERE sos_id = ?";
            String respSql = "INSERT INTO sos_responses (response_id, sos_id, responder_id, status) VALUES (?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql); PreparedStatement respStmt = conn.prepareStatement(respSql)) {
                    updateStmt.setString(1, sosId);
                    updateStmt.executeUpdate();
                    respStmt.setString(1, UUID.randomUUID().toString());
                    respStmt.setString(2, sosId);
                    respStmt.setString(3, responderId);
                    respStmt.setString(4, "RESPONDING");
                    respStmt.executeUpdate();
                    conn.commit();
                } catch (SQLException e) { conn.rollback(); throw e; }
                finally { conn.setAutoCommit(true); }

                cacheManager.getSosAlertCache().invalidate(sosId);
                cacheManager.invalidateActiveSOS();
                return getSOSAlertById(sosId);
            } catch (SQLException e) { throw new DatabaseException("Acknowledge error: " + e.getMessage()); }
        }

        public List<SOSAlert> getActiveSOSAlerts() throws DatabaseException {
            List<SOSAlert> cached = cacheManager.getActiveSOSListCache().getIfPresent("active_sos");
            if (cached != null) return cached;
            List<SOSAlert> alerts = new ArrayList<>();
            String sql = "SELECT * FROM sos_alerts WHERE status = 'ACTIVE' ORDER BY created_at DESC";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SOSAlert sos = new SOSAlert(rs.getString("sos_id"), rs.getString("user_id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("urgency_level"), rs.getString("description"));
                    sos.setStatus(rs.getString("status"));
                    sos.setRespondersCount(rs.getInt("responders_count"));
                    alerts.add(sos);
                }
                cacheManager.getActiveSOSListCache().put("active_sos", alerts);
                return alerts;
            } catch (SQLException e) { throw new DatabaseException("Query error: " + e.getMessage()); }
        }

        public SOSAlert getSOSAlertById(String sosId) throws DatabaseException {
            SOSAlert cached = cacheManager.getSosAlertCache().getIfPresent(sosId);
            if (cached != null) return cached;
            String sql = "SELECT * FROM sos_alerts WHERE sos_id = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sosId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        SOSAlert sos = new SOSAlert(rs.getString("sos_id"), rs.getString("user_id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("urgency_level"), rs.getString("description"));
                        sos.setStatus(rs.getString("status"));
                        sos.setRespondersCount(rs.getInt("responders_count"));
                        cacheManager.getSosAlertCache().put(sosId, sos);
                        return sos;
                    }
                }
                return null;
            } catch (SQLException e) { throw new DatabaseException("Query error: " + e.getMessage()); }
        }
    }

    public static class DisasterReportService {
        private static final Logger log = LoggerFactory.getLogger(DisasterReportService.class);
        private final DatabaseManager dbManager = DatabaseManager.getInstance();
        private final CaffeineCacheManager cacheManager = CaffeineCacheManager.getInstance();
        private final AlertService alertService;

        public DisasterReportService(AlertService alertService) { this.alertService = alertService; }

        public DisasterReport submitDisasterReport(String userId, String type, double lat, double lon, String severity, String desc, int affected) throws ValidationException, DatabaseException {
            InputValidator.validateDisasterReport(type, severity, desc, affected);
            InputValidator.validateLocation(lat, lon);
            String reportId = UUID.randomUUID().toString();
            DisasterReport report = new DisasterReport(reportId, userId, type, lat, lon, severity, desc, affected);

            String sql = "INSERT INTO disaster_reports (report_id, user_id, disaster_type, latitude, longitude, severity, description, status, affected_people) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, reportId);
                pstmt.setString(2, userId);
                pstmt.setString(3, type);
                pstmt.setDouble(4, lat);
                pstmt.setDouble(5, lon);
                pstmt.setString(6, severity);
                pstmt.setString(7, desc);
                pstmt.setString(8, "PENDING");
                pstmt.setInt(9, affected);
                pstmt.executeUpdate();

                alertService.broadcastAlert(new Alert(UUID.randomUUID().toString(), "DISASTER_UPDATE", type + " reported at (" + lat + ", " + lon + "). Severity: " + severity, severity));
                cacheManager.invalidateDisasterReports();
                log.info("Submitted disaster report: {}", reportId);
                return report;
            } catch (SQLException e) { throw new DatabaseException("Submit report error: " + e.getMessage()); }
        }

        public DisasterReport verifyDisasterReport(String reportId, String verifierId) throws DatabaseException {
            String sql = "UPDATE disaster_reports SET status = 'VERIFIED', verified_by = ?, verified_at = CURRENT_TIMESTAMP WHERE report_id = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, verifierId);
                pstmt.setString(2, reportId);
                pstmt.executeUpdate();
                cacheManager.invalidateDisasterReports();
                return getDisasterReportById(reportId);
            } catch (SQLException e) { throw new DatabaseException("Verify report error: " + e.getMessage()); }
        }

        public DisasterReport getDisasterReportById(String reportId) throws DatabaseException {
            String sql = "SELECT * FROM disaster_reports WHERE report_id = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, reportId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        DisasterReport report = new DisasterReport(rs.getString("report_id"), rs.getString("user_id"), rs.getString("disaster_type"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("severity"), rs.getString("description"), rs.getInt("affected_people"));
                        report.setStatus(rs.getString("status"));
                        report.setVerifiedBy(rs.getString("verified_by"));
                        return report;
                    }
                }
                return null;
            } catch (SQLException e) { throw new DatabaseException("Query report error: " + e.getMessage()); }
        }
    }

    public static class MetricsService {
        private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        private final Counter loginAttemptsCounter = Counter.builder("sdrp.login.attempts").register(registry);
        private final Counter sosCreatedCounter = Counter.builder("sdrp.sos.created").register(registry);
        private final Counter reportsSubmittedCounter = Counter.builder("sdrp.reports.submitted").register(registry);
        private static MetricsService instance;

        private MetricsService() {}
        public static synchronized MetricsService getInstance() {
            if (instance == null) instance = new MetricsService();
            return instance;
        }
        public void incrementLoginAttempts() { loginAttemptsCounter.increment(); }
        public void incrementSosCreated() { sosCreatedCounter.increment(); }
        public void incrementReportsSubmitted() { reportsSubmittedCounter.increment(); }
        public String scrapeMetrics() { return registry.scrape(); }
    }

    // =========================================================================
    // HTTP SERVER
    // =========================================================================
    public static class SdrpHttpServer {
        private final int port;
        private Server server;
        private final AlertService alertService = new AlertService();
        private final AuthenticationService authService = new AuthenticationService();
        private final SOSService sosService = new SOSService(alertService);
        private final DisasterReportService disasterService = new DisasterReportService(alertService);
        private final MetricsService metricsService = MetricsService.getInstance();
        private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        public SdrpHttpServer(int port) { this.port = port; }

        public void start() throws Exception {
            server = new Server(port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            context.addServlet(new ServletHolder(new HealthServlet()), "/health");
            context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
            context.addServlet(new ServletHolder(new RegisterServlet()), "/api/users/register");
            context.addServlet(new ServletHolder(new LoginServlet()), "/api/users/login");
            context.addServlet(new ServletHolder(new CreateSOSServlet()), "/api/sos/create");
            context.addServlet(new ServletHolder(new AcknowledgeSOSServlet()), "/api/sos/acknowledge");
            context.addServlet(new ServletHolder(new ActiveSOSServlet()), "/api/sos/active");
            context.addServlet(new ServletHolder(new SubmitDisasterServlet()), "/api/disaster/report");
            context.addServlet(new ServletHolder(new VerifyDisasterServlet()), "/api/disaster/verify");

            server.start();
            logger.info("SDRP Jetty HTTP Server started on port {}", port);
        }

        public void stop() throws Exception { if (server != null) server.stop(); }

        private void writeJsonResponse(HttpServletResponse resp, int status, boolean success, Object data, String error, String errorCode) throws IOException {
            resp.setStatus(status);
            resp.setContentType("application/json");
            Map<String, Object> body = new HashMap<>();
            body.put("success", success);
            if (success) body.put("data", data);
            else { body.put("error", error); body.put("errorCode", errorCode != null ? errorCode : "INTERNAL_ERROR"); }
            objectMapper.writeValue(resp.getWriter(), body);
        }

        private class HealthServlet extends HttpServlet {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                writeJsonResponse(resp, HttpServletResponse.SC_OK, true, Map.of("status", "UP", "service", "SDRP Backend"), null, null);
            }
        }

        private class MetricsServlet extends HttpServlet {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/plain");
                resp.getWriter().write(metricsService.scrapeMetrics());
            }
        }

        private class RegisterServlet extends HttpServlet {
            @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    Map map = objectMapper.readValue(req.getInputStream(), Map.class);
                    User user = authService.register((String) map.get("username"), (String) map.get("email"), (String) map.get("password"), (String) map.getOrDefault("userType", "VICTIM"), ((Number) map.getOrDefault("latitude", 0.0)).doubleValue(), ((Number) map.getOrDefault("longitude", 0.0)).doubleValue());
                    writeJsonResponse(resp, HttpServletResponse.SC_CREATED, true, user, null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, false, null, e.getMessage(), e.getErrorCode()); }
                catch (Exception e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), "SERVER_ERROR"); }
            }
        }

        private class LoginServlet extends HttpServlet {
            @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    metricsService.incrementLoginAttempts();
                    Map map = objectMapper.readValue(req.getInputStream(), Map.class);
                    User user = authService.login((String) map.get("username"), (String) map.get("password"));
                    writeJsonResponse(resp, HttpServletResponse.SC_OK, true, user, null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, false, null, e.getMessage(), e.getErrorCode()); }
                catch (Exception e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), "SERVER_ERROR"); }
            }
        }

        private class CreateSOSServlet extends HttpServlet {
            @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    metricsService.incrementSosCreated();
                    Map map = objectMapper.readValue(req.getInputStream(), Map.class);
                    SOSAlert sos = sosService.createSOSAlert((String) map.get("userId"), ((Number) map.get("latitude")).doubleValue(), ((Number) map.get("longitude")).doubleValue(), (String) map.get("urgencyLevel"), (String) map.get("description"));
                    writeJsonResponse(resp, HttpServletResponse.SC_CREATED, true, sos, null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, false, null, e.getMessage(), e.getErrorCode()); }
                catch (Exception e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), "SERVER_ERROR"); }
            }
        }

        private class AcknowledgeSOSServlet extends HttpServlet {
            @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    Map map = objectMapper.readValue(req.getInputStream(), Map.class);
                    SOSAlert sos = sosService.acknowledgeSOSAlert((String) map.get("sosId"), (String) map.get("responderId"));
                    writeJsonResponse(resp, HttpServletResponse.SC_OK, true, sos, null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, false, null, e.getMessage(), e.getErrorCode()); }
                catch (Exception e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), "SERVER_ERROR"); }
            }
        }

        private class ActiveSOSServlet extends HttpServlet {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    writeJsonResponse(resp, HttpServletResponse.SC_OK, true, sosService.getActiveSOSAlerts(), null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), e.getErrorCode()); }
            }
        }

        private class SubmitDisasterServlet extends HttpServlet {
            @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    metricsService.incrementReportsSubmitted();
                    Map map = objectMapper.readValue(req.getInputStream(), Map.class);
                    DisasterReport report = disasterService.submitDisasterReport((String) map.get("userId"), (String) map.get("disasterType"), ((Number) map.get("latitude")).doubleValue(), ((Number) map.get("longitude")).doubleValue(), (String) map.get("severity"), (String) map.get("description"), ((Number) map.getOrDefault("affectedPeople", 0)).intValue());
                    writeJsonResponse(resp, HttpServletResponse.SC_CREATED, true, report, null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, false, null, e.getMessage(), e.getErrorCode()); }
                catch (Exception e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), "SERVER_ERROR"); }
            }
        }

        private class VerifyDisasterServlet extends HttpServlet {
            @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    Map map = objectMapper.readValue(req.getInputStream(), Map.class);
                    DisasterReport report = disasterService.verifyDisasterReport((String) map.get("reportId"), (String) map.get("verifierId"));
                    writeJsonResponse(resp, HttpServletResponse.SC_OK, true, report, null, null);
                } catch (SDRPException e) { writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, false, null, e.getMessage(), e.getErrorCode()); }
                catch (Exception e) { writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, null, e.getMessage(), "SERVER_ERROR"); }
            }
        }
    }

    // =========================================================================
    // CLI TOOL
    // =========================================================================
    @Command(name = "sdrp-cli", mixinStandardHelpOptions = true, version = "1.0.0", description = "SDRP CLI Management Tool")
    public static class SdrpCli implements Callable<Integer> {
        @Override public Integer call() { System.out.println("SDRP CLI Tool"); return 0; }

        @Command(name = "start", description = "Start backend server")
        static class StartCommand implements Callable<Integer> {
            @Option(names = {"-p", "--port"}, defaultValue = "8085") int port;
            @Override public Integer call() {
                try {
                    SdrpHttpServer server = new SdrpHttpServer(port);
                    server.start();
                    Thread.currentThread().join();
                    return 0;
                } catch (Exception e) { return 1; }
            }
        }
    }
}

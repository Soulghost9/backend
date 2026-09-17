-- ============================================================================
-- ALVINA3548X DATABASE INTEGRATED SCHEMA FOR SDRP
-- Compatible with UserDAO, SosDAO, IncidentDAO & SDRP Engine
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL DEFAULT 'VICTIM',
    latitude DOUBLE DEFAULT 0.0,
    longitude DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS user_profiles (
    profile_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(100),
    blood_group VARCHAR(10),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sos_requests (
    sos_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    urgency_level VARCHAR(20) DEFAULT 'CRITICAL',
    description VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'Active',
    responders_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS gps_locations (
    location_id VARCHAR(36) PRIMARY KEY,
    sos_id VARCHAR(36) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sos_id) REFERENCES sos_requests(sos_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS incident_reports (
    report_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    disaster_type VARCHAR(30) NOT NULL,
    severity_level INT DEFAULT 1,
    severity VARCHAR(20) DEFAULT 'HIGH',
    description VARCHAR(1000) NOT NULL,
    latitude DOUBLE DEFAULT 0.0,
    longitude DOUBLE DEFAULT 0.0,
    affected_people INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    verified_by VARCHAR(36),
    verified_at TIMESTAMP,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Core SDRP Engine Compatibility Tables
CREATE TABLE IF NOT EXISTS sos_alerts (
    sos_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    urgency_level VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    responders_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sos_responses (
    response_id VARCHAR(36) PRIMARY KEY,
    sos_id VARCHAR(36) NOT NULL,
    responder_id VARCHAR(36) NOT NULL,
    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'RESPONDING'
);

CREATE TABLE IF NOT EXISTS disaster_reports (
    report_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    disaster_type VARCHAR(30) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',
    affected_people INT DEFAULT 0,
    verified_by VARCHAR(36),
    verified_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alerts (
    alert_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_acknowledged BOOLEAN DEFAULT FALSE
);

-- Sample Data Seeding
MERGE INTO users (user_id, username, email, password_hash, user_type, latitude, longitude, is_active)
VALUES ('user-001', 'john_responder', 'john@example.com', '8d969eef6ecad3c29a3a873fba5b4cd9fdb20ac89f3ccc36fafeb4f0f79c1be', 'RESPONDER', 12.9716, 77.5946, true);

MERGE INTO users (user_id, username, email, password_hash, user_type, latitude, longitude, is_active)
VALUES ('user-002', 'sarah_victim', 'sarah@example.com', '8d969eef6ecad3c29a3a873fba5b4cd9fdb20ac89f3ccc36fafeb4f0f79c1be', 'VICTIM', 12.9716, 77.5946, true);

MERGE INTO users (user_id, username, email, password_hash, user_type, latitude, longitude, is_active)
VALUES ('user-003', 'admin_user', 'admin@example.com', '8d969eef6ecad3c29a3a873fba5b4cd9fdb20ac89f3ccc36fafeb4f0f79c1be', 'ADMIN', 12.9716, 77.5946, true);

MERGE INTO user_profiles (profile_id, user_id, full_name, blood_group)
VALUES ('prof-001', 'user-001', 'John Smith', 'O+');

MERGE INTO sos_requests (sos_id, user_id, urgency_level, description, status)
VALUES ('sos-001', 'user-002', 'CRITICAL', 'Person trapped under collapsed building', 'Active');

MERGE INTO gps_locations (location_id, sos_id, latitude, longitude)
VALUES ('loc-001', 'sos-001', 12.9716, 77.5946);

MERGE INTO incident_reports (report_id, user_id, disaster_type, severity_level, severity, description, latitude, longitude, affected_people, status)
VALUES ('report-001', 'user-002', 'EARTHQUAKE', 3, 'HIGH', 'Major earthquake in downtown area', 12.9716, 77.5946, 150, 'PENDING');

MERGE INTO sos_alerts (sos_id, user_id, latitude, longitude, urgency_level, description, status)
VALUES ('sos-001', 'user-002', 12.9716, 77.5946, 'CRITICAL', 'Person trapped under collapsed building', 'ACTIVE');

MERGE INTO disaster_reports (report_id, user_id, disaster_type, latitude, longitude, severity, description, affected_people, status)
VALUES ('report-001', 'user-002', 'EARTHQUAKE', 12.9716, 77.5946, 'HIGH', 'Major earthquake in downtown area', 150, 'PENDING');

MERGE INTO alerts (alert_id, type, message, priority)
VALUES ('alert-001', 'DISASTER_UPDATE', 'Earthquake alert: High magnitude earthquake detected', 'CRITICAL');

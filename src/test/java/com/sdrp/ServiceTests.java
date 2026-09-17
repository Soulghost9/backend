package com.sdrp;

import com.sdrp.SDRPBackendComplete.AlertService;
import com.sdrp.SDRPBackendComplete.AuthenticationException;
import com.sdrp.SDRPBackendComplete.AuthenticationService;
import com.sdrp.SDRPBackendComplete.DisasterReport;
import com.sdrp.SDRPBackendComplete.DisasterReportService;
import com.sdrp.SDRPBackendComplete.SOSAlert;
import com.sdrp.SDRPBackendComplete.SOSService;
import com.sdrp.SDRPBackendComplete.User;
import com.sdrp.SDRPBackendComplete.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ServiceTests {

    private AlertService alertService;
    private AuthenticationService authService;
    private SOSService sosService;
    private DisasterReportService disasterService;

    @BeforeEach
    public void setup() {
        alertService = new AlertService();
        authService = new AuthenticationService();
        sosService = new SOSService(alertService);
        disasterService = new DisasterReportService(alertService);
    }

    private String randomUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    public void testUserRegistrationAndLogin() throws Exception {
        String username = randomUsername("usr");
        String email = username + "@test.com";

        User user = authService.register(username, email, "Password123", "RESPONDER", 12.9716, 77.5946);
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);

        User loggedIn = authService.login(username, "Password123");
        assertThat(loggedIn).isNotNull();
        assertThat(loggedIn.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    public void testInvalidPasswordLoginThrowsException() {
        assertThatThrownBy(() -> authService.login("nonexistent", "WrongPassword"))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void testInvalidUserRegistrationThrowsValidationException() {
        assertThatThrownBy(() -> authService.register("a", "invalid-email", "123", "RESPONDER", 0, 0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    public void testSOSCreationAndAcknowledgment() throws Exception {
        String victimName = randomUsername("victim");
        String responderName = randomUsername("resp");

        User victim = authService.register(victimName, victimName + "@test.com", "Password123", "VICTIM", 12.9716, 77.5946);
        User responder = authService.register(responderName, responderName + "@test.com", "Password123", "RESPONDER", 12.9716, 77.5946);

        SOSAlert sos = sosService.createSOSAlert(victim.getUserId(), 12.9716, 77.5946, "CRITICAL", "Trapped in building");

        assertThat(sos).isNotNull();
        assertThat(sos.getStatus()).isEqualTo("ACTIVE");

        List<SOSAlert> active = sosService.getActiveSOSAlerts();
        assertThat(active).isNotEmpty();

        SOSAlert acked = sosService.acknowledgeSOSAlert(sos.getSosId(), responder.getUserId());
        assertThat(acked.getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(acked.getRespondersCount()).isEqualTo(1);
    }

    @Test
    public void testDisasterReportSubmissionAndVerification() throws Exception {
        String victimName = randomUsername("reporter");
        String adminName = randomUsername("admin");

        User victim = authService.register(victimName, victimName + "@test.com", "Password123", "VICTIM", 12.9716, 77.5946);
        User admin = authService.register(adminName, adminName + "@test.com", "Password123", "ADMIN", 12.9716, 77.5946);

        DisasterReport report = disasterService.submitDisasterReport(victim.getUserId(), "EARTHQUAKE", 12.9716, 77.5946, "HIGH", "Severe tremors felt", 25);

        assertThat(report).isNotNull();
        assertThat(report.getStatus()).isEqualTo("PENDING");

        DisasterReport verified = disasterService.verifyDisasterReport(report.getReportId(), admin.getUserId());
        assertThat(verified.getStatus()).isEqualTo("VERIFIED");
    }
}

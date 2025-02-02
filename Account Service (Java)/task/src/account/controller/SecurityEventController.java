package account.controller;

import account.model.SecurityEvent;
import account.service.SecurityEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityEventController {

    @Autowired
    private SecurityEventService securityEventService;

    /**
     * GET /api/security/events
     *
     * Returns all security events sorted in ascending order by ID.
     * This endpoint is accessible only to users with the AUDITOR role.
     */
    @PreAuthorize("hasRole('AUDITOR')")
    @GetMapping("/events")
    public ResponseEntity<List<SecurityEvent>> getEvents() {
        List<SecurityEvent> events = securityEventService.getAllEvents();
        return ResponseEntity.ok(events);
    }
}

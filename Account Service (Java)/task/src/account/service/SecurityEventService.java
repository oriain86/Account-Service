package account.service;

import account.model.SecurityEvent;
import java.util.List;

public interface SecurityEventService {
    /**
     * Logs a security event.
     *
     * @param action  the event name (e.g. CREATE_USER, CHANGE_PASSWORD, etc.)
     * @param subject the user who performed the action (or "Anonymous")
     * @param object  the target object (for example, an email or API path)
     * @param path    the API endpoint path where the event occurred
     */
    void logEvent(String action, String subject, String object, String path);

    /**
     * Retrieves all security events.
     *
     * @return a list of all events
     */
    List<SecurityEvent> getAllEvents();
}

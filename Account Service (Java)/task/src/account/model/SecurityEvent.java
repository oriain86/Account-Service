package account.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;

    // For example: CREATE_USER, CHANGE_PASSWORD, etc.
    private String action;

    // The user who performed the action; if not available, "Anonymous"
    private String subject;

    // The object on which the action was performed.
    @Column(name = "object")
    private String eventObject;

    // The API path where the event occurred.
    private String path;

    public SecurityEvent() {
    }

    public SecurityEvent(LocalDateTime date, String action, String subject, String eventObject, String path) {
        this.date = date;
        this.action = action;
        this.subject = subject;
        this.eventObject = eventObject;
        this.path = path;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEventObject() {
        return eventObject;
    }
    public void setEventObject(String eventObject) {
        this.eventObject = eventObject;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
}

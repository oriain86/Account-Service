package account.model;

import jakarta.persistence.*;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lastname;

    // Email is unique and used as login (stored in lowercase)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // New fields for brute force detection
    private int failedLoginAttempts = 0;

    // When true, the account is locked.
    private boolean locked = false;

    // A user has one or more roles; we use a sorted set for consistent ordering.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<Group> roles = new TreeSet<>((g1, g2) -> g1.getName().compareTo(g2.getName()));

    public User() { }

    public User(String name, String lastname, String email, String password) {
        this.name = name;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLastname() { return lastname; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Set<Group> getRoles() { return roles; }
    public void setName(String name) { this.name = name; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRoles(Set<Group> roles) { this.roles = roles; }

    // Failed login attempts
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    // Account lock flag
    public boolean isLocked() {
        return locked;
    }
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}

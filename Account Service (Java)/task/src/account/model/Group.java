package account.model;

import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // For example: "ROLE_ADMINISTRATOR", "ROLE_ACCOUNTANT", "ROLE_USER"
    @Column(unique = true, nullable = false)
    private String name;

    public Group() {}

    public Group(String name) {
        this.name = name;
    }

    // Getters & setters
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}

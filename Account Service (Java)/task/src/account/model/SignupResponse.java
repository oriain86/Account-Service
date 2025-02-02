package account.model;

public class SignupResponse {
    private Long id;
    private String name;
    private String lastname;
    private String email;

    public SignupResponse(Long id, String name, String lastname, String email) {
        this.id = id;
        this.name = name;
        this.lastname = lastname;
        this.email = email;
    }

    // Getters
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getLastname() {
        return lastname;
    }
    public String getEmail() {
        return email;
    }
}

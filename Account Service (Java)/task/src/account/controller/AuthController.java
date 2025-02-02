package account.controller;

import account.model.Group;
import account.model.SignupRequest;
import account.model.User;
import account.repository.GroupRepository;
import account.repository.UserRepository;
import account.service.SecurityEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // List of breached passwords (for testing purposes)
    private static final Set<String> BREACHED_PASSWORDS = Set.of(
            "PasswordForJanuary", "PasswordForFebruary", "PasswordForMarch", "PasswordForApril",
            "PasswordForMay", "PasswordForJune", "PasswordForJuly", "PasswordForAugust",
            "PasswordForSeptember", "PasswordForOctober", "PasswordForNovember", "PasswordForDecember"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Service responsible for logging security events
    @Autowired
    private SecurityEventService securityEventService;

    /**
     * Helper method to build error responses.
     */
    private ResponseEntity<Map<String, Object>> buildError(String message, String path, int status) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorDetails.put("status", status);
        String errorText;
        if (status == 403) {
            errorText = "Forbidden";
        } else if (status == 404) {
            errorText = "Not Found";
        } else {
            errorText = "Bad Request";
        }
        errorDetails.put("error", errorText);
        errorDetails.put("message", message);
        errorDetails.put("path", path);
        return ResponseEntity.status(status).body(errorDetails);
    }

    /**
     * POST /api/auth/signup
     *
     * Registers a new user. The first user to register is assigned the Administrator role;
     * all subsequent users receive the User role.
     *
     * In addition, a security event with action CREATE_USER is logged.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();

        // Ensure the email ends with @acme.com (case-insensitive)
        if (!request.getEmail().toLowerCase().endsWith("@acme.com")) {
            return buildError("Email must end with @acme.com", path, 400);
        }

        // Check for duplicate email (case-insensitive)
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            return buildError("User exist!", path, 400);
        }

        // Validate password length
        if (request.getPassword().length() < 12) {
            return buildError("Password length must be 12 chars minimum!", path, 400);
        }
        // Validate against breached passwords
        if (BREACHED_PASSWORDS.contains(request.getPassword())) {
            return buildError("The password is in the hacker's database!", path, 400);
        }

        // Create new user and encode the password
        User user = new User();
        user.setName(request.getName());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Role assignment: if this is the first user, assign Administrator role; otherwise, assign User role.
        if (userRepository.count() == 0) {
            Group adminGroup = groupRepository.findByName("ROLE_ADMINISTRATOR")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMINISTRATOR not found"));
            user.getRoles().add(adminGroup);
        } else {
            Group userGroup = groupRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
            user.getRoles().add(userGroup);
        }

        user = userRepository.save(user);

        // Log the security event: successful user registration.
        // For signup, the subject is "Anonymous" because the user did not have an account yet.
        securityEventService.logEvent("CREATE_USER", "Anonymous", user.getEmail(), path);

        // Build response with sorted roles.
        List<String> roleNames = new ArrayList<>();
        user.getRoles().forEach(g -> roleNames.add(g.getName()));
        Collections.sort(roleNames);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("lastname", user.getLastname());
        response.put("email", user.getEmail());
        response.put("roles", roleNames);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/changepass
     *
     * Changes the password of an authenticated user.
     *
     * A security event with action CHANGE_PASSWORD is logged upon success.
     */
    @PostMapping("/changepass")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body,
                                            HttpServletRequest httpRequest,
                                            Authentication authentication) {
        String path = httpRequest.getRequestURI();
        String newPassword = body.get("new_password");

        // Check that new_password is provided and meets the minimum length requirement.
        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 12) {
            return buildError("Password length must be 12 chars minimum!", path, 400);
        }
        // Check that the new password is not in the breached list.
        if (BREACHED_PASSWORDS.contains(newPassword)) {
            return buildError("The password is in the hacker's database!", path, 400);
        }

        // Retrieve the authenticated user.
        String email = authentication.getName();
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            return buildError("User not found!", path, 400);
        }
        User user = optionalUser.get();

        // Ensure that the new password is different from the current one.
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return buildError("The passwords must be different!", path, 400);
        }

        // Update the password and save.
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Log the security event for a successful password change.
        securityEventService.logEvent("CHANGE_PASSWORD", user.getEmail(), user.getEmail(), path);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("email", user.getEmail());
        response.put("status", "The password has been updated successfully");
        return ResponseEntity.ok(response);
    }
}

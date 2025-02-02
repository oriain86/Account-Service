package account.controller;

import account.model.Group;
import account.model.User;
import account.repository.GroupRepository;
import account.repository.UserRepository;
import account.service.SecurityEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private SecurityEventService securityEventService;

    /**
     * GET /api/admin/user
     * Returns a list of all users (non-sensitive information), sorted by ID in ascending order.
     * Accessible only by an Administrator.
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/user")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        String path = request.getRequestURI();
        List<User> users = userRepository.findAll();
        users.sort(Comparator.comparing(User::getId));
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("lastname", user.getLastname());
            userMap.put("email", user.getEmail());
            List<String> roleNames = new ArrayList<>();
            user.getRoles().forEach(g -> roleNames.add(g.getName()));
            Collections.sort(roleNames);
            userMap.put("roles", roleNames);
            result.add(userMap);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/admin/user/{email}
     * Deletes the specified user.
     * The Administrator cannot delete himself (or any user with the ROLE_ADMINISTRATOR).
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/user/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email, HttpServletRequest request,
                                        @AuthenticationPrincipal UserDetails authUser) {
        String path = request.getRequestURI();
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            return buildError("User not found!", path, 404);
        }
        User user = optionalUser.get();
        // Prevent deletion of the administrator (or the current admin)
        if (user.getEmail().equalsIgnoreCase(authUser.getUsername()) ||
                user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMINISTRATOR"))) {
            return buildError("Can't remove ADMINISTRATOR role!", path, 400);
        }
        userRepository.delete(user);
        securityEventService.logEvent("DELETE_USER", authUser.getUsername(), user.getEmail(), path);
        Map<String, String> response = new HashMap<>();
        response.put("user", user.getEmail());
        response.put("status", "Deleted successfully!");
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/user/role
     * Grants or removes a role for a specified user.
     *
     * Request JSON:
     * {
     *    "user": "<user email>",
     *    "role": "<role (e.g. ACCOUNTANT or USER)>",
     *    "operation": "<GRANT or REMOVE>"
     * }
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PutMapping("/user/role")
    public ResponseEntity<?> changeUserRole(@RequestBody Map<String, String> body, HttpServletRequest request,
                                            @AuthenticationPrincipal UserDetails authUser) {
        String path = request.getRequestURI();
        String email = body.get("user");
        String roleStr = body.get("role");
        String operation = body.get("operation");

        if (email == null || roleStr == null || operation == null) {
            return buildError("Invalid input!", path, 400);
        }

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            return buildError("User not found!", path, 404);
        }
        User user = optionalUser.get();

        String roleName = "ROLE_" + roleStr.toUpperCase();  // For example, ROLE_ACCOUNTANT or ROLE_USER
        Optional<Group> optionalGroup = groupRepository.findByName(roleName);
        if (optionalGroup.isEmpty()) {
            return buildError("Role not found!", path, 404);
        }
        Group group = optionalGroup.get();

        // Validate grouping: administrative vs. business roles.
        boolean userHasAdmin = user.getRoles().stream().anyMatch(g ->
                g.getName().equals("ROLE_ADMINISTRATOR"));
        boolean isBusinessRole = roleName.equals("ROLE_ACCOUNTANT") || roleName.equals("ROLE_USER");
        if (operation.equalsIgnoreCase("GRANT")) {
            // If user is administrative, don't allow granting business roles
            if (userHasAdmin && isBusinessRole) {
                return buildError("The user cannot combine administrative and business roles!",
                        path, 400);
            }
            user.getRoles().add(group);
            securityEventService.logEvent("GRANT_ROLE", authUser.getUsername(),
                    "Grant role " + roleStr.toUpperCase() + " to " + email, path);
        } else if (operation.equalsIgnoreCase("REMOVE")) {
            if (user.getRoles().stream().noneMatch(g -> g.getName().equals(roleName))) {
                return buildError("The user does not have a role!", path, 400);
            }
            if (user.getRoles().size() == 1) {
                return buildError("The user must have at least one role!", path, 400);
            }
            if (roleName.equals("ROLE_ADMINISTRATOR")) {
                return buildError("Can't remove ADMINISTRATOR role!", path, 400);
            }
            user.getRoles().removeIf(g -> g.getName().equals(roleName));
            securityEventService.logEvent("REMOVE_ROLE", authUser.getUsername(),
                    "Remove role " + roleStr.toUpperCase() + " from " + email, path);
        } else {
            return buildError("Invalid operation!", path, 400);
        }
        userRepository.save(user);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("lastname", user.getLastname());
        response.put("email", user.getEmail());
        List<String> roleNames = new ArrayList<>();
        user.getRoles().forEach(g -> roleNames.add(g.getName()));
        Collections.sort(roleNames);
        response.put("roles", roleNames);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/user/access
     * Locks or unlocks a user.
     *
     * Request JSON:
     * {
     *    "user": "<user email>",
     *    "operation": "<LOCK or UNLOCK>"
     * }
     *
     * The Administrator cannot be locked.
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PutMapping("/user/access")
    public ResponseEntity<?> updateUserAccess(@RequestBody Map<String, String> body, HttpServletRequest request,
                                              @AuthenticationPrincipal UserDetails authUser) {
        String path = request.getRequestURI();
        String email = body.get("user");
        String operation = body.get("operation");

        if (email == null || operation == null) {
            return buildError("Invalid input!", path, 400);
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return buildError("User not found!", path, 404);
        }
        User user = userOpt.get();

        // Do not allow locking an administrator.
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMINISTRATOR"))) {
            return buildError("Can't lock the ADMINISTRATOR!", path, 400);
        }

        if (operation.equalsIgnoreCase("LOCK")) {
            user.setLocked(true);
            securityEventService.logEvent("LOCK_USER", authUser.getUsername(),
                    "Lock user " + email, path);
        } else if (operation.equalsIgnoreCase("UNLOCK")) {
            user.setLocked(false);
            user.setFailedLoginAttempts(0);
            securityEventService.logEvent("UNLOCK_USER", authUser.getUsername(),
                    "Unlock user " + email, path);
        } else {
            return buildError("Invalid operation!", path, 400);
        }
        userRepository.save(user);
        Map<String, String> response = new HashMap<>();
        response.put("status", "User " + email + " " + (operation.equalsIgnoreCase("LOCK")
                ? "locked" : "unlocked") + "!");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildError(String message, String path, int status) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorDetails.put("status", status);
        String errorText = (status == 400) ? "Bad Request" : (status == 404) ? "Not Found" : "";
        errorDetails.put("error", errorText);
        errorDetails.put("message", message);
        errorDetails.put("path", path);
        return ResponseEntity.status(status).body(errorDetails);
    }
}

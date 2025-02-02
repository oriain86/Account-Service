package account.security;

import account.model.User;
import account.repository.UserRepository;
import account.service.SecurityEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityEventService securityEventService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("username"); // username parameter from login form
        String path = request.getRequestURI();

        // Log LOGIN_FAILED event
        securityEventService.logEvent("LOGIN_FAILED", email != null ? email : "Anonymous", path, path);

        // Check if the exception is due to bad credentials and if email is provided.
        if (email != null) {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Increase the counter if the account is not already locked.
                if (!user.isLocked()) {
                    int failedAttempts = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(failedAttempts);
                    // If maximum failed attempts reached, log brute force and lock user.
                    if (failedAttempts > MAX_FAILED_ATTEMPTS) {
                        securityEventService.logEvent("BRUTE_FORCE", email, path, path);
                        user.setLocked(true);
                        securityEventService.logEvent("LOCK_USER", email, "Lock user " + email, path);
                    }
                    userRepository.save(user);
                }
            }
        }

        // Return a 401 response
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{ \"error\": \"Authentication failed\" }");
    }
}

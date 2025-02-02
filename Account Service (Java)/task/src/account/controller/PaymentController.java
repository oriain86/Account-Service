package account.controller;

import account.model.SignupResponse;
import account.model.User;
import account.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empl")
public class PaymentController {

    @Autowired
    private UserRepository userRepository;

    // Changed mapping to avoid conflict
    @GetMapping("/user")
    public ResponseEntity<SignupResponse> getPayment(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        SignupResponse response = new SignupResponse(user.getId(), user.getName(),
                user.getLastname(), user.getEmail());
        return ResponseEntity.ok(response);
    }
}

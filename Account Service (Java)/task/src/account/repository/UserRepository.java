package account.repository;

import account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Lookup by email ignoring case
    Optional<User> findByEmailIgnoreCase(String email);
}

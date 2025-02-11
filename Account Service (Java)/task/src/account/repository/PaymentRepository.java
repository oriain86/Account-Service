package account.repository;

import account.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByEmployeeIgnoreCaseAndPeriod(String employee, String period);
    List<Payment> findByEmployeeIgnoreCase(String employee);
}

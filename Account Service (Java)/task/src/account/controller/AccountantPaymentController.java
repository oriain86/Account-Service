package account.controller;

import account.model.Payment;
import account.model.User;
import account.repository.PaymentRepository;
import account.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class AccountantPaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    // A regular expression for the period format "mm-YYYY"
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])-\\d{4}$");

    // Helper method to build error responses
    private ResponseEntity<Map<String, Object>> buildError(String message, String path) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorDetails.put("status", 400);
        errorDetails.put("error", "Bad Request");
        errorDetails.put("message", message);
        errorDetails.put("path", path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    // --------------------------------------
    // POST /api/acct/payments – upload payrolls
    // --------------------------------------

    @PostMapping("/api/acct/payments")
    @Transactional
    public ResponseEntity<?> uploadPayments(@RequestBody List<Map<String, Object>> payments,
                                            HttpServletRequest request) {
        String path = request.getRequestURI();
        List<String> errors = new ArrayList<>();

        // Validate each payment record
        for (int i = 0; i < payments.size(); i++) {
            Map<String, Object> paymentMap = payments.get(i);
            String prefix = "payments[" + i + "].";

            // Extract fields
            String employee = (String) paymentMap.get("employee");
            String period = (String) paymentMap.get("period");
            Object salaryObj = paymentMap.get("salary");

            // Validate employee exists
            if (employee == null || userRepository.findByEmailIgnoreCase(employee).isEmpty()) {
                errors.add(prefix + "employee: Employee not found");
            }
            // Validate period format
            if (period == null || !PERIOD_PATTERN.matcher(period).matches()) {
                errors.add(prefix + "period: Wrong date!");
            }
            // Validate salary (nonnegative)
            Long salary = null;
            try {
                salary = Long.parseLong(paymentMap.get("salary").toString());
            } catch (Exception e) {
                // salary remains null
            }
            if (salary == null || salary < 0) {
                errors.add(prefix + "salary: Salary must be non negative!");
            }
            // Check that for this employee and period the record does not already exist
            if (employee != null && period != null &&
                    PERIOD_PATTERN.matcher(period).matches() &&
                    salary != null && salary >= 0) {
                if (paymentRepository.findByEmployeeIgnoreCaseAndPeriod(employee, period).isPresent()) {
                    errors.add(prefix + "employee and period: Duplicate record");
                }
            }
        }

        if (!errors.isEmpty()) {
            String errorMessage = String.join(", ", errors);
            return buildError(errorMessage, path);
        }

        // Save all valid payment records
        List<Payment> toSave = new ArrayList<>();
        for (Map<String, Object> paymentMap : payments) {
            String employee = ((String) paymentMap.get("employee")).toLowerCase();
            String period = (String) paymentMap.get("period");
            Long salary = Long.parseLong(paymentMap.get("salary").toString());
            toSave.add(new Payment(employee, period, salary));
        }
        paymentRepository.saveAll(toSave);
        Map<String, String> response = new HashMap<>();
        response.put("status", "Added successfully!");
        return ResponseEntity.ok(response);
    }

    // --------------------------------------
    // PUT /api/acct/payments – update salary for a specific record
    // --------------------------------------

    @PutMapping("/api/acct/payments")
    public ResponseEntity<?> updatePayment(@RequestBody Map<String, Object> paymentMap,
                                           HttpServletRequest request) {
        String path = request.getRequestURI();
        List<String> errors = new ArrayList<>();

        String employee = (String) paymentMap.get("employee");
        String period = (String) paymentMap.get("period");
        Object salaryObj = paymentMap.get("salary");

        if (employee == null || userRepository.findByEmailIgnoreCase(employee).isEmpty()) {
            errors.add("employee: Employee not found");
        }
        if (period == null || !PERIOD_PATTERN.matcher(period).matches()) {
            errors.add("period: Wrong date!");
        }
        Long salary = null;
        try {
            salary = Long.parseLong(paymentMap.get("salary").toString());
        } catch (Exception e) { }
        if (salary == null || salary < 0) {
            errors.add("salary: Salary must be non negative!");
        }

        if (!errors.isEmpty()) {
            String errorMessage = String.join(", ", errors);
            return buildError(errorMessage, path);
        }

        // Find the payment record (for update, record must exist)
        Optional<Payment> optionalPayment = paymentRepository.
                findByEmployeeIgnoreCaseAndPeriod(employee, period);
        if (optionalPayment.isEmpty()) {
            return buildError("Payment record not found", path);
        }
        Payment payment = optionalPayment.get();
        payment.setSalary(salary);
        paymentRepository.save(payment);

        Map<String, String> response = new HashMap<>();
        response.put("status", "Updated successfully!");
        return ResponseEntity.ok(response);
    }

    // --------------------------------------
    // GET /api/empl/payment – get payroll information for an employee
    // --------------------------------------

    @GetMapping("/api/empl/payment")
    public ResponseEntity<?> getPayments(@RequestParam(required = false) String period,
                                         Authentication authentication,
                                         HttpServletRequest request) {
        String path = request.getRequestURI();
        String email = authentication.getName();
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userOptional.get();

        // Retrieve all payments for this employee
        List<Payment> payments = paymentRepository.findByEmployeeIgnoreCase(email);

        // If a period is specified, validate and filter for that record
        if (period != null) {
            if (!PERIOD_PATTERN.matcher(period).matches()) {
                return buildError("Wrong date!", path);
            }
            Optional<Payment> paymentOpt = payments.stream()
                    .filter(p -> p.getPeriod().equals(period))
                    .findFirst();
            if (paymentOpt.isEmpty()) {
                // Return an empty JSON object if no data is found for the given period
                return ResponseEntity.ok(new LinkedHashMap<>());
            }
            Payment p = paymentOpt.get();
            Map<String, String> result = new LinkedHashMap<>();
            result.put("name", user.getName());
            result.put("lastname", user.getLastname());
            result.put("period", formatPeriod(p.getPeriod()));
            result.put("salary", formatSalary(p.getSalary()));
            return ResponseEntity.ok(result);
        } else {
            // No period specified: return a list of payments sorted descending by date.
            // Since period is in "mm-YYYY", we sort by year then month descending.
            List<Payment> sortedPayments = payments.stream().sorted((p1, p2) -> {
                String[] parts1 = p1.getPeriod().split("-");
                String[] parts2 = p2.getPeriod().split("-");
                int month1 = Integer.parseInt(parts1[0]);
                int year1 = Integer.parseInt(parts1[1]);
                int month2 = Integer.parseInt(parts2[0]);
                int year2 = Integer.parseInt(parts2[1]);
                if (year1 != year2) {
                    return Integer.compare(year2, year1);
                } else {
                    return Integer.compare(month2, month1);
                }
            }).collect(Collectors.toList());

            List<Map<String, String>> resultList = new ArrayList<>();
            for (Payment p : sortedPayments) {
                Map<String, String> result = new LinkedHashMap<>();
                result.put("name", user.getName());
                result.put("lastname", user.getLastname());
                result.put("period", formatPeriod(p.getPeriod()));
                result.put("salary", formatSalary(p.getSalary()));
                resultList.add(result);
            }
            return ResponseEntity.ok(resultList);
        }
    }

    // Convert period from "mm-YYYY" to "MonthName-YYYY"
    private String formatPeriod(String period) {
        String[] parts = period.split("-");
        int month = Integer.parseInt(parts[0]);
        String year = parts[1];
        String monthName;
        switch (month) {
            case 1: monthName = "January"; break;
            case 2: monthName = "February"; break;
            case 3: monthName = "March"; break;
            case 4: monthName = "April"; break;
            case 5: monthName = "May"; break;
            case 6: monthName = "June"; break;
            case 7: monthName = "July"; break;
            case 8: monthName = "August"; break;
            case 9: monthName = "September"; break;
            case 10: monthName = "October"; break;
            case 11: monthName = "November"; break;
            case 12: monthName = "December"; break;
            default: monthName = "Unknown"; break;
        }
        return monthName + "-" + year;
    }

    // Convert salary in cents to a string "X dollar(s) Y cent(s)"
    private String formatSalary(Long salary) {
        long dollars = salary / 100;
        long cents = salary % 100;
        return String.format("%d dollar(s) %d cent(s)", dollars, cents);
    }
}

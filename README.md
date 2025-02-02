# Project: Account Service

## Background

The **Account Service** is a secure web application built using Spring Boot. It provides user registration, authentication, and business functionality for managing user accounts, roles, payments, and security events. The project is designed with security best practices in mind and includes measures such as:

- **Secure password management:** Password length validation, breached password checking, and bcrypt hashing.
- **Role-based access control:** Supports roles for Anonymous, User, Accountant, Administrator, and Auditor.
- **Brute-force protection:** Detects repeated failed login attempts and locks accounts as needed.
- **HTTPS support:** Uses a self-signed certificate (generated via keytool) for secure communication (for training purposes).

## Features

### User Registration & Authentication
- New users can register via `POST /api/auth/signup`.
- Passwords are validated and stored securely using BCrypt (work factor ≥ 13).

### Password Change
- Authenticated users can change their password via `POST /api/auth/changepass`.

### Payment Management

#### Accountant Endpoints:
- `POST /api/acct/payments` – Upload payroll data (transactional with validation).
- `PUT /api/acct/payments` – Update a specific payroll record.

#### Employee Endpoint:
- `GET /api/empl/payment` – Retrieve payroll information (with optional period filtering).

### Role Management (Administrator Only)
- `GET /api/admin/user` – List all users (non-sensitive info).
- `PUT /api/admin/user/role` – Grant or remove roles from users.
- `DELETE /api/admin/user/{email}` – Delete a user (with restrictions on administrators).
- `PUT /api/admin/user/access` – Lock or unlock a user account.

### Security Event Logging (Auditor Only)
- All security-relevant actions are logged (e.g., user creation, password changes, login failures, role changes, account locking).
- `GET /api/security/events` – Auditor endpoint to retrieve all security events.

### HTTPS Support
- The application supports HTTPS using a self-signed certificate (generated via keytool).

## Technologies Used

- **Spring Boot 3.x** for application development.
- **Spring Security** for authentication and authorization.
- **Spring Data JPA** for persistence with an H2 database.
- **Gradle** for build and dependency management.
- **H2 Database** for in-memory (or file-based) data persistence.
- **HTTPS** enabled with a self-signed certificate (PKCS12 keystore).


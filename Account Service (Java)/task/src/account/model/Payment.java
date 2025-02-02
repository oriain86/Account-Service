package account.model;

import jakarta.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Employee’s email (stored in lowercase)
    private String employee;

    // Period in the format "mm-YYYY"
    private String period;

    // Salary in cents (nonnegative)
    private Long salary;

    public Payment() {}

    public Payment(String employee, String period, Long salary) {
        this.employee = employee;
        this.period = period;
        this.salary = salary;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getSalary() {
        return salary;
    }

    public void setSalary(Long salary) {
        this.salary = salary;
    }
}

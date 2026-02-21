package com.shotaroi.loan.underwriting;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 10, scale = 8)
    private BigDecimal annualInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    protected LoanApplication() {}

    public LoanApplication(Long customerId, BigDecimal principal, String currency,
                           Integer termMonths, BigDecimal annualInterestRate) {
        this.customerId = customerId;
        this.principal = principal;
        this.currency = currency;
        this.termMonths = termMonths;
        this.annualInterestRate = annualInterestRate;
        this.status = ApplicationStatus.SUBMITTED;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Optional<Instant> getDecidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    public Optional<String> getDecisionReason() {
        return Optional.ofNullable(decisionReason);
    }

    public void approve(String reason) {
        this.status = ApplicationStatus.APPROVED;
        this.decidedAt = Instant.now();
        this.decisionReason = reason;
    }

    public void reject(String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.decidedAt = Instant.now();
        this.decisionReason = reason;
    }

    public enum ApplicationStatus {
        SUBMITTED,
        APPROVED,
        REJECTED
    }
}

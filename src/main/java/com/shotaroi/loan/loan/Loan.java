package com.shotaroi.loan.loan;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 10, scale = 8)
    private BigDecimal annualInterestRate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "accrued_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal accruedInterest;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Loan() {}

    public Loan(Long customerId, Long applicationId, BigDecimal principal, String currency,
                Integer termMonths, BigDecimal annualInterestRate,
                LocalDate startDate, LocalDate endDate) {
        this.customerId = customerId;
        this.applicationId = applicationId;
        this.principal = principal;
        this.currency = currency;
        this.termMonths = termMonths;
        this.annualInterestRate = annualInterestRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = LoanStatus.ACTIVE;
        this.outstandingPrincipal = principal;
        this.accruedInterest = BigDecimal.ZERO.setScale(2);
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getApplicationId() {
        return applicationId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public BigDecimal getAccruedInterest() {
        return accruedInterest;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) {
        this.outstandingPrincipal = outstandingPrincipal;
    }

    public void setAccruedInterest(BigDecimal accruedInterest) {
        this.accruedInterest = accruedInterest;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public enum LoanStatus {
        ACTIVE,
        CLOSED,
        DEFAULTED
    }
}

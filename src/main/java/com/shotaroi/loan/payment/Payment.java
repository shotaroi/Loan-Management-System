package com.shotaroi.loan.payment;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "reference")
    private String reference;

    @Column(name = "allocated_to_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedToInterest;

    @Column(name = "allocated_to_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedToPrincipal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(Long loanId, BigDecimal amount, String currency, LocalDate paymentDate,
                   String reference, BigDecimal allocatedToInterest, BigDecimal allocatedToPrincipal) {
        this.loanId = loanId;
        this.amount = amount;
        this.currency = currency;
        this.paymentDate = paymentDate;
        this.reference = reference;
        this.allocatedToInterest = allocatedToInterest;
        this.allocatedToPrincipal = allocatedToPrincipal;
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getAllocatedToInterest() {
        return allocatedToInterest;
    }

    public BigDecimal getAllocatedToPrincipal() {
        return allocatedToPrincipal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

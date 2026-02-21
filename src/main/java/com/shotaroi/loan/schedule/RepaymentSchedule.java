package com.shotaroi.loan.schedule;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment_schedule")
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalDue;

    @Column(name = "interest_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestDue;

    @Column(name = "total_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    private BigDecimal amountPaid;

    protected RepaymentSchedule() {}

    public RepaymentSchedule(Long loanId, int installmentNumber, LocalDate dueDate,
                             BigDecimal principalDue, BigDecimal interestDue,
                             BigDecimal totalDue) {
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.principalDue = principalDue;
        this.interestDue = interestDue;
        this.totalDue = totalDue;
        this.status = InstallmentStatus.DUE;
        this.amountPaid = BigDecimal.ZERO.setScale(2);
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getPrincipalDue() {
        return principalDue;
    }

    public BigDecimal getInterestDue() {
        return interestDue;
    }

    public BigDecimal getTotalDue() {
        return totalDue;
    }

    public InstallmentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid != null ? amountPaid : BigDecimal.ZERO.setScale(2);
    }

    public void setStatus(InstallmentStatus status) {
        this.status = status;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getRemainingDue() {
        return totalDue.subtract(getAmountPaid());
    }

    public enum InstallmentStatus {
        DUE,
        PAID,
        LATE
    }
}

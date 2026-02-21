package com.shotaroi.loan.payment;

import com.shotaroi.loan.common.exception.ForbiddenException;
import com.shotaroi.loan.common.exception.ResourceNotFoundException;
import com.shotaroi.loan.common.exception.ValidationException;
import com.shotaroi.loan.common.validation.LoanValidation;
import com.shotaroi.loan.loan.Loan;
import com.shotaroi.loan.loan.LoanRepository;
import com.shotaroi.loan.schedule.RepaymentSchedule;
import com.shotaroi.loan.schedule.RepaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int SCALE = 2;

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          LoanRepository loanRepository,
                          RepaymentScheduleRepository scheduleRepository) {
        this.paymentRepository = paymentRepository;
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public PaymentResult postPayment(Long loanId, Long customerId, BigDecimal amount, String currency,
                                     LocalDate paymentDate, String reference) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!loan.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Access denied to this loan");
        }
        if (loan.getStatus() == Loan.LoanStatus.CLOSED) {
            throw new ValidationException("Cannot post payment to closed loan");
        }

        LoanValidation.validatePaymentAmount(amount);
        LoanValidation.validateCurrencyMatch(loan.getCurrency(), currency);

        BigDecimal remaining = amount.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal allocatedToInterest = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal allocatedToPrincipal = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal outstandingPrincipal = loan.getOutstandingPrincipal();
        BigDecimal accruedInterest = loan.getAccruedInterest();

        if (remaining.compareTo(BigDecimal.ZERO) > 0 && accruedInterest.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal toInterest = remaining.min(accruedInterest);
            allocatedToInterest = allocatedToInterest.add(toInterest);
            remaining = remaining.subtract(toInterest);
            accruedInterest = accruedInterest.subtract(toInterest);
        }

        List<RepaymentSchedule> installments = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        for (RepaymentSchedule s : installments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (s.getStatus() == RepaymentSchedule.InstallmentStatus.PAID) continue;

            BigDecimal amountPaid = s.getAmountPaid();
            BigDecimal interestRemaining = s.getInterestDue().subtract(amountPaid.min(s.getInterestDue()));
            BigDecimal principalRemaining = s.getPrincipalDue().subtract(
                    amountPaid.subtract(amountPaid.min(s.getInterestDue())).max(BigDecimal.ZERO));

            BigDecimal toPayInterest = remaining.min(interestRemaining);
            BigDecimal toPayPrincipal = remaining.subtract(toPayInterest).min(principalRemaining);
            BigDecimal toPay = toPayInterest.add(toPayPrincipal);

            if (toPay.compareTo(BigDecimal.ZERO) > 0) {
                allocatedToInterest = allocatedToInterest.add(toPayInterest);
                allocatedToPrincipal = allocatedToPrincipal.add(toPayPrincipal);
                accruedInterest = accruedInterest.subtract(toPayInterest);
                outstandingPrincipal = outstandingPrincipal.subtract(toPayPrincipal);

                s.setAmountPaid(amountPaid.add(toPay));
                if (s.getAmountPaid().compareTo(s.getTotalDue()) >= 0) {
                    s.setStatus(RepaymentSchedule.InstallmentStatus.PAID);
                }
                scheduleRepository.save(s);
                remaining = remaining.subtract(toPay);
            }
        }

        loan.setAccruedInterest(accruedInterest.max(BigDecimal.ZERO));
        loan.setOutstandingPrincipal(outstandingPrincipal.max(BigDecimal.ZERO));

        if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
            log.info("Loan closed: id={}", loanId);
        }
        loanRepository.save(loan);

        Payment payment = new Payment(loanId, amount, currency, paymentDate, reference,
                allocatedToInterest, allocatedToPrincipal);
        payment = paymentRepository.save(payment);

        log.info("Payment posted: loanId={}, amount={}, toInterest={}, toPrincipal={}, newOutstanding={}",
                loanId, amount, allocatedToInterest, allocatedToPrincipal, loan.getOutstandingPrincipal());

        return new PaymentResult(payment.getId(), allocatedToInterest, allocatedToPrincipal, loan.getOutstandingPrincipal());
    }

    public List<Payment> getPayments(Long loanId) {
        return paymentRepository.findByLoanIdOrderByCreatedAtDesc(loanId);
    }

    public record PaymentResult(Long paymentId, BigDecimal allocatedToInterest,
                                BigDecimal allocatedToPrincipal, BigDecimal newOutstandingPrincipal) {}
}

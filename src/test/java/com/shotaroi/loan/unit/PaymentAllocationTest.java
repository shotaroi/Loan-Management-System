package com.shotaroi.loan.unit;

import com.shotaroi.loan.loan.Loan;
import com.shotaroi.loan.loan.LoanRepository;
import com.shotaroi.loan.payment.Payment;
import com.shotaroi.loan.payment.PaymentRepository;
import com.shotaroi.loan.payment.PaymentService;
import com.shotaroi.loan.schedule.RepaymentSchedule;
import com.shotaroi.loan.schedule.RepaymentScheduleRepository;
import com.shotaroi.loan.schedule.ScheduleCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAllocationTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    private PaymentService paymentService;

    private Loan loan;
    private List<RepaymentSchedule> installments;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, loanRepository, scheduleRepository);

        loan = new Loan(1L, 1L, new BigDecimal("12000.00"), "SEK", 12,
                new BigDecimal("0.12"), LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1));
        loan.setOutstandingPrincipal(new BigDecimal("12000.00"));
        loan.setAccruedInterest(BigDecimal.ZERO);

        var scheduleData = ScheduleCalculator.generateSchedule(
                new BigDecimal("12000.00"), new BigDecimal("0.12"), 12, LocalDate.of(2025, 1, 1));
        installments = new ArrayList<>();
        for (var i : scheduleData) {
            RepaymentSchedule s = new RepaymentSchedule(1L, i.installmentNumber(), i.dueDate(),
                    i.principalDue(), i.interestDue(), i.totalDue());
            installments.add(s);
        }
    }

    @Test
    void payment_pays_interest_first_then_principal() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(1L)).thenReturn(installments);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new Payment(p.getLoanId(), p.getAmount(), p.getCurrency(), p.getPaymentDate(),
                    p.getReference(), p.getAllocatedToInterest(), p.getAllocatedToPrincipal());
        });
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(RepaymentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = paymentService.postPayment(1L, 1L,
                new BigDecimal("1200.00"), "SEK",
                LocalDate.of(2025, 2, 1), "ref1");

        assertThat(result.allocatedToInterest()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.allocatedToPrincipal()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.allocatedToInterest().add(result.allocatedToPrincipal()))
                .isEqualByComparingTo(new BigDecimal("1200.00"));

        RepaymentSchedule first = installments.get(0);
        assertThat(first.getInterestDue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.allocatedToInterest()).as("Interest paid first, then principal").isGreaterThanOrEqualTo(first.getInterestDue());
    }

    @Test
    void multiple_payments_mark_installments_paid_in_order() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(1L)).thenReturn(installments);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new Payment(p.getLoanId(), p.getAmount(), p.getCurrency(), p.getPaymentDate(),
                    p.getReference(), p.getAllocatedToInterest(), p.getAllocatedToPrincipal());
        });
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            loan.setOutstandingPrincipal(l.getOutstandingPrincipal());
            loan.setAccruedInterest(l.getAccruedInterest());
            loan.setStatus(l.getStatus());
            return l;
        });
        when(scheduleRepository.save(any(RepaymentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal firstInstallmentTotal = installments.get(0).getTotalDue();

        paymentService.postPayment(1L, 1L, firstInstallmentTotal, "SEK",
                LocalDate.of(2025, 2, 1), "ref1");

        assertThat(installments.get(0).getStatus()).isEqualTo(RepaymentSchedule.InstallmentStatus.PAID);
        assertThat(installments.get(1).getStatus()).isEqualTo(RepaymentSchedule.InstallmentStatus.DUE);
    }
}

package com.shotaroi.loan.schedule;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleService {

    private final RepaymentScheduleRepository scheduleRepository;

    public ScheduleService(RepaymentScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public ScheduleSummary getSummary(Long loanId) {
        List<RepaymentSchedule> installments = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        BigDecimal totalPaid = BigDecimal.ZERO.setScale(2);
        BigDecimal totalRemaining = BigDecimal.ZERO.setScale(2);
        int paidCount = 0;
        int pendingCount = 0;
        Optional<LocalDate> nextDueDate = Optional.empty();

        for (RepaymentSchedule s : installments) {
            if (s.getStatus() == RepaymentSchedule.InstallmentStatus.PAID) {
                totalPaid = totalPaid.add(s.getAmountPaid());
                paidCount++;
            } else {
                totalRemaining = totalRemaining.add(s.getRemainingDue());
                pendingCount++;
                if (nextDueDate.isEmpty()) {
                    nextDueDate = Optional.of(s.getDueDate());
                }
            }
        }

        return new ScheduleSummary(totalPaid, totalRemaining, nextDueDate, paidCount, pendingCount);
    }

    public void refreshLateStatus(Long loanId) {
        List<RepaymentSchedule> installments = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        LocalDate today = LocalDate.now();

        for (RepaymentSchedule s : installments) {
            if (s.getStatus() == RepaymentSchedule.InstallmentStatus.DUE
                    && s.getDueDate().isBefore(today)) {
                s.setStatus(RepaymentSchedule.InstallmentStatus.LATE);
                scheduleRepository.save(s);
            }
        }
    }

    public record ScheduleSummary(BigDecimal totalPaid, BigDecimal totalRemaining,
                                  Optional<LocalDate> nextDueDate, int paidCount, int pendingCount) {}
}

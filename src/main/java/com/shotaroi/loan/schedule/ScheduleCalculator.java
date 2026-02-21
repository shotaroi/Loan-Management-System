package com.shotaroi.loan.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates amortization schedule using fixed monthly installment (annuity) formula.
 * Payment = P * r / (1 - (1+r)^-n)
 * For r=0: equal principal payments.
 * Rounding: last installment principal is adjusted to ensure total principal sums exactly.
 */
public final class ScheduleCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private ScheduleCalculator() {}

    public static List<ScheduleInstallment> generateSchedule(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            int termMonths,
            LocalDate startDate) {

        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), 16, ROUNDING);
        List<ScheduleInstallment> installments = new ArrayList<>(termMonths);

        BigDecimal monthlyPayment;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = principal.divide(BigDecimal.valueOf(termMonths), SCALE, ROUNDING);
        } else {
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
            BigDecimal factor = BigDecimal.ONE.subtract(
                    onePlusR.pow(-termMonths, new java.math.MathContext(16)));
            monthlyPayment = principal.multiply(monthlyRate).divide(factor, 16, ROUNDING);
        }

        BigDecimal remainingPrincipal = principal.setScale(SCALE, ROUNDING);
        LocalDate dueDate = startDate.plusMonths(1);

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(SCALE, ROUNDING);
            BigDecimal principalDue;

            if (i == termMonths) {
                principalDue = remainingPrincipal;
                BigDecimal totalDue = principalDue.add(interestDue);
                installments.add(new ScheduleInstallment(i, dueDate, principalDue, interestDue, totalDue));
            } else {
                principalDue = monthlyPayment.subtract(interestDue).setScale(SCALE, ROUNDING);
                BigDecimal totalDue = principalDue.add(interestDue);
                installments.add(new ScheduleInstallment(i, dueDate, principalDue, interestDue, totalDue));
                remainingPrincipal = remainingPrincipal.subtract(principalDue);
            }
            dueDate = dueDate.plusMonths(1);
        }

        BigDecimal principalSum = installments.stream()
                .map(ScheduleInstallment::principalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal delta = principal.subtract(principalSum).setScale(SCALE, ROUNDING);
        if (delta.abs().compareTo(BigDecimal.ZERO) > 0) {
            ScheduleInstallment last = installments.get(installments.size() - 1);
            BigDecimal adjustedPrincipal = last.principalDue().add(delta);
            BigDecimal adjustedTotal = adjustedPrincipal.add(last.interestDue());
            installments.set(installments.size() - 1,
                    new ScheduleInstallment(last.installmentNumber(), last.dueDate(),
                            adjustedPrincipal, last.interestDue(), adjustedTotal));
        }

        return installments;
    }

    public record ScheduleInstallment(
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal principalDue,
            BigDecimal interestDue,
            BigDecimal totalDue
    ) {}
}

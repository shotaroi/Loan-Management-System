package com.shotaroi.loan.unit;

import com.shotaroi.loan.schedule.ScheduleCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleCalculatorTest {

    @Test
    void r0_case_produces_equal_principal_payments() {
        BigDecimal principal = new BigDecimal("12000.00");
        BigDecimal annualRate = BigDecimal.ZERO;
        int termMonths = 12;
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        List<ScheduleCalculator.ScheduleInstallment> schedule = ScheduleCalculator.generateSchedule(
                principal, annualRate, termMonths, startDate);

        assertThat(schedule).hasSize(12);

        BigDecimal expectedPrincipalPerMonth = new BigDecimal("1000.00");
        for (int i = 0; i < 11; i++) {
            assertThat(schedule.get(i).principalDue()).isEqualByComparingTo(expectedPrincipalPerMonth);
            assertThat(schedule.get(i).interestDue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
        assertThat(schedule.get(11).principalDue()).isEqualByComparingTo(expectedPrincipalPerMonth);
        assertThat(schedule.get(11).interestDue()).isEqualByComparingTo(BigDecimal.ZERO);

        BigDecimal totalPrincipal = schedule.stream()
                .map(ScheduleCalculator.ScheduleInstallment::principalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo(principal);
    }

    @Test
    void r0_case_due_dates_increment_monthly() {
        BigDecimal principal = new BigDecimal("6000.00");
        LocalDate startDate = LocalDate.of(2025, 3, 15);

        List<ScheduleCalculator.ScheduleInstallment> schedule = ScheduleCalculator.generateSchedule(
                principal, BigDecimal.ZERO, 6, startDate);

        assertThat(schedule.get(0).dueDate()).isEqualTo(LocalDate.of(2025, 4, 15));
        assertThat(schedule.get(1).dueDate()).isEqualTo(LocalDate.of(2025, 5, 15));
        assertThat(schedule.get(5).dueDate()).isEqualTo(LocalDate.of(2025, 9, 15));
    }

    @Test
    void r_positive_case_principal_sums_to_original_within_tolerance() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("0.05");
        int termMonths = 36;
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        List<ScheduleCalculator.ScheduleInstallment> schedule = ScheduleCalculator.generateSchedule(
                principal, annualRate, termMonths, startDate);

        assertThat(schedule).hasSize(36);

        BigDecimal totalPrincipal = schedule.stream()
                .map(ScheduleCalculator.ScheduleInstallment::principalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalPrincipal).isEqualByComparingTo(principal);
    }

    @Test
    void r_positive_case_first_installment_has_more_interest_than_last() {
        BigDecimal principal = new BigDecimal("50000.00");
        BigDecimal annualRate = new BigDecimal("0.12");
        int termMonths = 12;
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        List<ScheduleCalculator.ScheduleInstallment> schedule = ScheduleCalculator.generateSchedule(
                principal, annualRate, termMonths, startDate);

        assertThat(schedule.get(0).interestDue()).isGreaterThan(schedule.get(11).interestDue());
        assertThat(schedule.get(0).principalDue()).isLessThan(schedule.get(11).principalDue());
    }

    @Test
    void r_positive_case_total_principal_exact_after_last_installment_adjustment() {
        BigDecimal principal = new BigDecimal("33333.33");
        BigDecimal annualRate = new BigDecimal("0.075");
        int termMonths = 24;
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        List<ScheduleCalculator.ScheduleInstallment> schedule = ScheduleCalculator.generateSchedule(
                principal, annualRate, termMonths, startDate);

        BigDecimal totalPrincipal = schedule.stream()
                .map(ScheduleCalculator.ScheduleInstallment::principalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalPrincipal).isEqualByComparingTo(principal);
    }
}

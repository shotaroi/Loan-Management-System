package com.shotaroi.loan.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    Page<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId, Pageable pageable);
}

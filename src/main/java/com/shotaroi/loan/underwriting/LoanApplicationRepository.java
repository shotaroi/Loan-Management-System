package com.shotaroi.loan.underwriting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByCustomerIdOrderBySubmittedAtDesc(Long customerId);

    Page<LoanApplication> findByStatusOrderBySubmittedAtAsc(
            LoanApplication.ApplicationStatus status,
            Pageable pageable);
}

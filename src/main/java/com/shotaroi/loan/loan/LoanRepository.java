package com.shotaroi.loan.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Loan> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);
}

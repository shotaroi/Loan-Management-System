package com.shotaroi.loan.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanIdOrderByCreatedAtDesc(Long loanId);
}

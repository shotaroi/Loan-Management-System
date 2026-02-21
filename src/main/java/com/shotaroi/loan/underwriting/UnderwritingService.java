package com.shotaroi.loan.underwriting;

import com.shotaroi.loan.common.exception.ForbiddenException;
import com.shotaroi.loan.common.exception.ResourceNotFoundException;
import com.shotaroi.loan.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UnderwritingService {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingService.class);

    private final LoanApplicationRepository applicationRepository;

    public UnderwritingService(LoanApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public LoanApplication createApplication(Long customerId, BigDecimal principal, String currency,
                                            int termMonths, BigDecimal annualInterestRate) {
        LoanApplication app = new LoanApplication(customerId, principal, currency, termMonths, annualInterestRate);
        return applicationRepository.save(app);
    }

    public LoanApplication getByIdAndCustomer(Long id, Long customerId) {
        LoanApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
        if (!app.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Access denied to this application");
        }
        return app;
    }

    public List<LoanApplication> findByCustomerId(Long customerId) {
        return applicationRepository.findByCustomerIdOrderBySubmittedAtDesc(customerId);
    }

    public LoanApplication getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }

    public Page<LoanApplication> findByStatus(LoanApplication.ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatusOrderBySubmittedAtAsc(status, pageable);
    }

    @Transactional
    public LoanApplication decide(Long id, LoanApplication.ApplicationStatus decision, String reason) {
        LoanApplication app = getById(id);
        if (app.getStatus() != LoanApplication.ApplicationStatus.SUBMITTED) {
            throw new ValidationException("Application already decided");
        }
        if (decision == LoanApplication.ApplicationStatus.APPROVED) {
            app.approve(reason);
        } else if (decision == LoanApplication.ApplicationStatus.REJECTED) {
            app.reject(reason);
        } else {
            throw new ValidationException("Decision must be APPROVE or REJECT");
        }
        return applicationRepository.save(app);
    }
}

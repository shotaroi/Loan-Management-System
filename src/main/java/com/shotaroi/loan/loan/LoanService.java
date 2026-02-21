package com.shotaroi.loan.loan;

import com.shotaroi.loan.common.exception.ForbiddenException;
import com.shotaroi.loan.common.exception.ResourceNotFoundException;
import com.shotaroi.loan.common.exception.ValidationException;
import com.shotaroi.loan.schedule.RepaymentSchedule;
import com.shotaroi.loan.schedule.RepaymentScheduleRepository;
import com.shotaroi.loan.schedule.ScheduleCalculator;
import com.shotaroi.loan.underwriting.LoanApplication;
import com.shotaroi.loan.underwriting.LoanApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final LoanApplicationRepository applicationRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    public LoanService(LoanRepository loanRepository,
                       LoanApplicationRepository applicationRepository,
                       RepaymentScheduleRepository scheduleRepository) {
        this.loanRepository = loanRepository;
        this.applicationRepository = applicationRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public Loan createFromApplication(Long applicationId, LocalDate startDate, Long customerId) {
        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!app.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Access denied to this application");
        }
        if (app.getStatus() != LoanApplication.ApplicationStatus.APPROVED) {
            throw new ValidationException("Only approved applications can be converted to loans");
        }
        if (loanRepository.existsByApplicationId(applicationId)) {
            throw new ValidationException("Loan already exists for this application");
        }

        LocalDate endDate = startDate.plusMonths(app.getTermMonths());

        Loan loan = new Loan(
                app.getCustomerId(),
                app.getId(),
                app.getPrincipal(),
                app.getCurrency(),
                app.getTermMonths(),
                app.getAnnualInterestRate(),
                startDate,
                endDate);

        loan = loanRepository.save(loan);
        final Long loanId = loan.getId();

        var installments = ScheduleCalculator.generateSchedule(
                app.getPrincipal(),
                app.getAnnualInterestRate(),
                app.getTermMonths(),
                startDate);

        List<RepaymentSchedule> schedule = installments.stream()
                .map(i -> new RepaymentSchedule(
                        loanId,
                        i.installmentNumber(),
                        i.dueDate(),
                        i.principalDue(),
                        i.interestDue(),
                        i.totalDue()))
                .collect(Collectors.toList());

        scheduleRepository.saveAll(schedule);

        log.info("Loan created: id={}, applicationId={}, termMonths={}", loan.getId(), applicationId, app.getTermMonths());

        return loan;
    }

    public Loan getByIdAndCustomer(Long id, Long customerId) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        if (!loan.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Access denied to this loan");
        }
        return loan;
    }

    public List<Loan> findByCustomerId(Long customerId) {
        return loanRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Loan getById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }
}

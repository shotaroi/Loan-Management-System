package com.shotaroi.loan.loan;

import com.shotaroi.loan.audit.AuditService;
import com.shotaroi.loan.payment.Payment;
import com.shotaroi.loan.payment.PaymentService;
import com.shotaroi.loan.schedule.RepaymentSchedule;
import com.shotaroi.loan.schedule.RepaymentScheduleRepository;
import com.shotaroi.loan.schedule.ScheduleService;
import com.shotaroi.loan.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loans")
public class LoanController {

    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    private final LoanService loanService;
    private final RepaymentScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final PaymentService paymentService;
    private final AuditService auditService;

    public LoanController(LoanService loanService,
                          RepaymentScheduleRepository scheduleRepository,
                          ScheduleService scheduleService,
                          PaymentService paymentService,
                          AuditService auditService) {
        this.loanService = loanService;
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
        this.paymentService = paymentService;
        this.auditService = auditService;
    }

    @PostMapping("/from-application/{applicationId}")
    @Operation(summary = "Create loan from approved application")
    public ResponseEntity<LoanResponse> createFromApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal SecurityUser user) {

        Loan loan = loanService.createFromApplication(applicationId, request.startDate(), user.getId());

        auditService.logSync(user.getId(), "LOAN_CREATED",
                "loanId=%d, applicationId=%d, principal=%s %s".formatted(loan.getId(), applicationId, loan.getPrincipal(), loan.getCurrency()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new LoanResponse(
                loan.getId(),
                loan.getStatus().name(),
                loan.getStartDate().toString(),
                loan.getEndDate().toString()));
    }

    @GetMapping
    @Operation(summary = "List own loans")
    public ResponseEntity<List<LoanResponse>> listOwn(@AuthenticationPrincipal SecurityUser user) {
        List<Loan> loans = loanService.findByCustomerId(user.getId());
        return ResponseEntity.ok(loans.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan details")
    public ResponseEntity<LoanDetailResponse> get(@PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        Loan loan = loanService.getByIdAndCustomer(id, user.getId());
        return ResponseEntity.ok(toDetailResponse(loan));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "List loan schedule (paginated)")
    public ResponseEntity<org.springframework.data.domain.Page<ScheduleItemResponse>> getSchedule(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUser user) {

        Loan loan = loanService.getByIdAndCustomer(id, user.getId());
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var schedulePage = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId(), pageable);

        return ResponseEntity.ok(schedulePage.map(this::toScheduleItem));
    }

    @PostMapping("/{id}/schedule/refresh")
    @Operation(summary = "Refresh late status on installments")
    public ResponseEntity<Void> refreshSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {
        Loan loan = loanService.getByIdAndCustomer(id, user.getId());
        scheduleService.refreshLateStatus(loan.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/schedule/summary")
    @Operation(summary = "Get schedule summary")
    public ResponseEntity<ScheduleSummaryResponse> getScheduleSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {

        Loan loan = loanService.getByIdAndCustomer(id, user.getId());
        var summary = scheduleService.getSummary(loan.getId());

        return ResponseEntity.ok(new ScheduleSummaryResponse(
                summary.totalPaid(),
                summary.totalRemaining(),
                summary.nextDueDate().map(LocalDate::toString).orElse(null),
                summary.paidCount(),
                summary.pendingCount()));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Post a payment")
    public ResponseEntity<PaymentResponse> postPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal SecurityUser user) {

        var result = paymentService.postPayment(id, user.getId(), request.amount(), request.currency(),
                request.paymentDate(), request.reference());

        auditService.logSync(user.getId(), "PAYMENT_POSTED",
                "loanId=%d, amount=%s, allocatedInterest=%s, allocatedPrincipal=%s".formatted(id, request.amount(), result.allocatedToInterest(), result.allocatedToPrincipal()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new PaymentResponse(
                result.paymentId(),
                result.allocatedToInterest(),
                result.allocatedToPrincipal(),
                result.newOutstandingPrincipal()));
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "List loan payments")
    public ResponseEntity<List<PaymentItemResponse>> listPayments(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {

        Loan loan = loanService.getByIdAndCustomer(id, user.getId());
        var payments = paymentService.getPayments(loan.getId());

        return ResponseEntity.ok(payments.stream()
                .map(p -> new PaymentItemResponse(
                        p.getId(),
                        p.getAmount(),
                        p.getPaymentDate().toString(),
                        p.getReference(),
                        p.getAllocatedToInterest(),
                        p.getAllocatedToPrincipal(),
                        p.getCreatedAt().toString()))
                .toList());
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getStatus().name(),
                loan.getStartDate().toString(),
                loan.getEndDate().toString());
    }

    private LoanDetailResponse toDetailResponse(Loan loan) {
        return new LoanDetailResponse(
                loan.getId(),
                loan.getStatus().name(),
                loan.getPrincipal(),
                loan.getCurrency(),
                loan.getOutstandingPrincipal(),
                loan.getAccruedInterest(),
                loan.getStartDate().toString(),
                loan.getEndDate().toString());
    }

    private ScheduleItemResponse toScheduleItem(RepaymentSchedule s) {
        return new ScheduleItemResponse(
                s.getInstallmentNumber(),
                s.getDueDate().toString(),
                s.getPrincipalDue(),
                s.getInterestDue(),
                s.getTotalDue(),
                s.getStatus().name(),
                s.getAmountPaid());
    }

    public record CreateLoanRequest(@NotNull LocalDate startDate) {}

    public record LoanResponse(Long loanId, String status, String startDate, String endDate) {}

    public record LoanDetailResponse(Long loanId, String status, BigDecimal principal, String currency,
                                     BigDecimal outstandingPrincipal, BigDecimal accruedInterest,
                                     String startDate, String endDate) {}

    public record ScheduleItemResponse(int installmentNumber, String dueDate, BigDecimal principalDue,
                                       BigDecimal interestDue, BigDecimal totalDue, String status,
                                       BigDecimal amountPaid) {}

    public record ScheduleSummaryResponse(BigDecimal totalPaid, BigDecimal totalRemaining,
                                         String nextDueDate, int paidCount, int pendingCount) {}

    public record PaymentRequest(@NotNull BigDecimal amount, @NotNull @jakarta.validation.constraints.Pattern(regexp = "^[A-Z]{3}$") String currency,
                                 @NotNull LocalDate paymentDate, String reference) {}

    public record PaymentResponse(Long paymentId, BigDecimal allocatedToInterest,
                                  BigDecimal allocatedToPrincipal, BigDecimal newOutstandingPrincipal) {}

    public record PaymentItemResponse(Long id, BigDecimal amount, String paymentDate, String reference,
                                      BigDecimal allocatedToInterest, BigDecimal allocatedToPrincipal,
                                      String createdAt) {}
}

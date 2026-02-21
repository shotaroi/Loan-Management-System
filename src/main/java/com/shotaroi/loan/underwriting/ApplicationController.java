package com.shotaroi.loan.underwriting;

import com.shotaroi.loan.audit.AuditService;
import com.shotaroi.loan.common.validation.LoanValidation;
import com.shotaroi.loan.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Loan Applications")
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    private final UnderwritingService underwritingService;
    private final AuditService auditService;

    public ApplicationController(UnderwritingService underwritingService, AuditService auditService) {
        this.underwritingService = underwritingService;
        this.auditService = auditService;
    }

    @PostMapping
    @Operation(summary = "Create a new loan application")
    public ResponseEntity<ApplicationResponse> create(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal SecurityUser user) {

        LoanValidation.validatePrincipal(request.principal());
        LoanValidation.validateTermMonths(request.termMonths());
        LoanValidation.validateAnnualInterestRate(request.annualInterestRate());
        LoanValidation.validateCurrency(request.currency());

        LoanApplication app = underwritingService.createApplication(
                user.getId(), request.principal(), request.currency(),
                request.termMonths(), request.annualInterestRate());

        auditService.logSync(user.getId(), "APPLICATION_SUBMITTED",
                "applicationId=%d, principal=%s %s".formatted(app.getId(), app.getPrincipal(), app.getCurrency()));

        log.info("Application created: id={}, customerId={}", app.getId(), user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(app));
    }

    @GetMapping
    @Operation(summary = "List own applications")
    public ResponseEntity<List<ApplicationResponse>> listOwn(@AuthenticationPrincipal SecurityUser user) {
        List<LoanApplication> apps = underwritingService.findByCustomerId(user.getId());
        return ResponseEntity.ok(apps.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details")
    public ResponseEntity<ApplicationResponse> get(@PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        LoanApplication app = underwritingService.getByIdAndCustomer(id, user.getId());
        return ResponseEntity.ok(toResponse(app));
    }

    private ApplicationResponse toResponse(LoanApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getPrincipal(),
                app.getCurrency(),
                app.getTermMonths(),
                app.getAnnualInterestRate(),
                app.getStatus().name(),
                app.getSubmittedAt().toString(),
                app.getDecidedAt().map(Object::toString).orElse(null),
                app.getDecisionReason().orElse(null));
    }

    public record ApplicationRequest(
            @NotNull BigDecimal principal,
            @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @NotNull @Min(3) @Max(360) Integer termMonths,
            @NotNull @DecimalMin("0") @DecimalMax("0.50") BigDecimal annualInterestRate
    ) {}

    public record ApplicationResponse(
            Long applicationId,
            java.math.BigDecimal principal,
            String currency,
            Integer termMonths,
            java.math.BigDecimal annualInterestRate,
            String status,
            String submittedAt,
            String decidedAt,
            String decisionReason
    ) {}
}

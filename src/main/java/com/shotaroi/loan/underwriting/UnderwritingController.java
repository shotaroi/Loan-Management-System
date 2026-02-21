package com.shotaroi.loan.underwriting;

import com.shotaroi.loan.audit.AuditService;
import com.shotaroi.loan.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/underwriting/applications")
@Tag(name = "Underwriting")
public class UnderwritingController {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingController.class);

    private final UnderwritingService underwritingService;
    private final AuditService auditService;

    public UnderwritingController(UnderwritingService underwritingService, AuditService auditService) {
        this.underwritingService = underwritingService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List submitted applications (UNDERWRITER)")
    public ResponseEntity<List<ApplicationResponse>> listSubmitted(
            @RequestParam(defaultValue = "SUBMITTED") LoanApplication.ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUser user) {

        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var pageResult = underwritingService.findByStatus(status, pageable);
        List<ApplicationResponse> list = pageResult.getContent().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Approve or reject application (UNDERWRITER)")
    public ResponseEntity<ApplicationResponse> decide(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal SecurityUser user) {

        LoanApplication app = underwritingService.decide(id, request.decision(), request.reason());

        auditService.logSync(user.getId(), "UNDERWRITING_DECISION",
                "applicationId=%d, decision=%s, reason=%s".formatted(id, request.decision(), request.reason()));

        log.info("Underwriting decision: applicationId={}, decision={}, by={}", id, request.decision(), user.getId());

        return ResponseEntity.ok(toResponse(app));
    }

    private ApplicationResponse toResponse(LoanApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCustomerId(),
                app.getPrincipal(),
                app.getCurrency(),
                app.getTermMonths(),
                app.getAnnualInterestRate(),
                app.getStatus().name(),
                app.getSubmittedAt().toString(),
                app.getDecidedAt().map(Object::toString).orElse(null),
                app.getDecisionReason().orElse(null));
    }

    public record DecisionRequest(
            @NotNull LoanApplication.ApplicationStatus decision,
            @NotBlank String reason
    ) {}

    public record ApplicationResponse(
            Long applicationId,
            Long customerId,
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

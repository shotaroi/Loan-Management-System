package com.shotaroi.loan.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Admin - Audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @Operation(summary = "List audit logs (paginated, ADMIN)")
    public ResponseEntity<AuditPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal com.shotaroi.loan.security.SecurityUser user) {

        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var auditPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AuditItemResponse> items = auditPage.getContent().stream()
                .map(a -> new AuditItemResponse(
                        a.getId(),
                        a.getActorCustomerId(),
                        a.getAction(),
                        a.getDetails(),
                        a.getCreatedAt().toString()))
                .toList();

        return ResponseEntity.ok(new AuditPageResponse(
                items,
                auditPage.getTotalElements(),
                auditPage.getTotalPages(),
                auditPage.getNumber()));
    }

    public record AuditItemResponse(Long id, Long actorCustomerId, String action, String details, String createdAt) {}

    public record AuditPageResponse(List<AuditItemResponse> content, long totalElements, int totalPages, int number) {}
}

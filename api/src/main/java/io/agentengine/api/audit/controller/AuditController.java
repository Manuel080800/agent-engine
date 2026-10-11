package io.agentengine.api.audit.controller;

import io.agentengine.core.audit.record.AuditResponseRecord;
import io.agentengine.core.audit.service.AuditService;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditResponseRecord> list(@RequestParam(defaultValue = "0") int offset) {
        return auditService.list(offset);
    }
}

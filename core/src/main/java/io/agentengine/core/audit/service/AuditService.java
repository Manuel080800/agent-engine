package io.agentengine.core.audit.service;

import io.agentengine.core.audit.record.AuditResponseRecord;

import java.util.List;

public interface AuditService {
    List<AuditResponseRecord> list(int offset);
}

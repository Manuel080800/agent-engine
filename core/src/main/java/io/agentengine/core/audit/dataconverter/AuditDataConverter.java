package io.agentengine.core.audit.dataconverter;

import io.agentengine.core.audit.domain.AuditEntry;
import io.agentengine.core.audit.record.AuditResponseRecord;

import org.springframework.stereotype.Component;

@Component
public class AuditDataConverter {
    public AuditResponseRecord toRecord(AuditEntry entity) {
        return new AuditResponseRecord(entity.getOperation(), entity.getResourceId(), entity.getCreatedAt());
    }
}

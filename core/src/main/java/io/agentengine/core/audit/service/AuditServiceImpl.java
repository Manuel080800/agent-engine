package io.agentengine.core.audit.service;

import io.agentengine.core.audit.dataconverter.AuditDataConverter;
import io.agentengine.core.audit.record.AuditResponseRecord;
import io.agentengine.core.audit.repository.AuditRepository;
import io.agentengine.core.common.pagination.OffsetPageRequest;
import io.agentengine.core.common.security.IdentitySecurity;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditRepository repository;
    private final AuditDataConverter converter;

    public AuditServiceImpl(AuditRepository repository, AuditDataConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditResponseRecord> list(int offset) {
        IdentitySecurity.human();
        var page = new OffsetPageRequest(Math.max(0, offset), 100, Sort.unsorted());
        var entries = repository.findByOwnerIdOrderByCreatedAtDescIdAsc(IdentitySecurity.owner(), page);
        return entries.stream().map(converter::toRecord).toList();
    }
}

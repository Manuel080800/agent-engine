package io.agentengine.core.common.service;

import io.agentengine.core.audit.domain.AuditEntry;
import io.agentengine.core.audit.repository.AuditRepository;
import io.agentengine.core.common.dataconverter.ResourceDataConverter;
import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.model.service.ModelConfigurationService;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.handler.ResourceKindHandler;
import io.agentengine.core.common.handler.ResourcePersistenceHandlerResolver;
import io.agentengine.core.common.handler.ResourceValidationHandler;
import io.agentengine.core.common.pagination.OffsetPageRequest;
import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.common.repository.dao.ResourceDAO;
import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.util.JsonCodec;

import java.time.Instant;
import java.util.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceServiceImpl implements ResourceService {
    private final ModelConfigurationService models;
    private final ResourceValidationHandler validationHandler;
    private final ResourceDAO resourceDAO;
    private final ResourcePersistenceHandlerResolver persistenceHandlers;
    private final ResourceDataConverter converter;
    private final AuditRepository auditRepository;
    private final JsonCodec json;

    public ResourceServiceImpl(ResourceDAO resourceDAO, ResourcePersistenceHandlerResolver persistenceHandlers, ResourceDataConverter converter, AuditRepository auditRepository, JsonCodec json, ResourceValidationHandler validationHandler, ModelConfigurationService models) {
        this.models = models;
        this.resourceDAO = resourceDAO;
        this.persistenceHandlers = persistenceHandlers;
        this.converter = converter;
        this.auditRepository = auditRepository;
        this.json = json;
        this.validationHandler = validationHandler;
    }

    @Override
    public String encode(Object value) {
        return json.encode(value);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResponseRecord> list(String kind, UUID project, String query, int limit, int offset, boolean tombstones) {
        if (Objects.nonNull(project)) {
            get("projects", project, false);
        }

        var resourceKind = ResourceKindHandler.resolve(kind);
        var pageSize = Math.min(Math.max(limit, 1), 100);
        var pageOffset = Math.max(offset, 0);
        List<? extends VersionedResource> entities;

        if (Objects.isNull(query) || query.isBlank()) {
            var pageable = new OffsetPageRequest(pageOffset, pageSize, Sort.unsorted());
            entities = persistenceHandlers.resolve(resourceKind).list(IdentitySecurity.owner(), project, tombstones, pageable);
        } else {
            entities = resourceDAO.search(resourceKind, IdentitySecurity.owner(), project, query, pageSize, pageOffset, tombstones);
        }

        return converter.toRecords(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResponseRecord> all(String kind, UUID project) {
        var result = new ArrayList<ResourceResponseRecord>();

        for (var offset = 0;; offset += 100) {
            var page = list(kind, project, "", 100, offset, false);
            result.addAll(page);

            if (page.size() < 100) {
                return result;
            }

            if (offset >= 9900) {
                throw new DomainException(DomainError.CONFLICT, "Context exceeds resource limit; reduce scope");
            }
        }
    }

    @Override
    @Transactional
    public ResourceResponseRecord get(String kind, UUID id, boolean lock) {
        var resourceKind = ResourceKindHandler.resolve(kind);
        return converter.toRecord(findEntity(resourceKind, id, lock));
    }

    @Transactional
    @Override
    public ResourceResponseRecord create(String kind, ResourceWriteRecord<Map<String, Object>> write) {
        validationHandler.validate(kind, write.data());

        if (kind.equals("projects") && Objects.nonNull(write.projectId())) {
            throw new IllegalArgumentException("Projects cannot be nested");
        }

        if (Objects.nonNull(write.projectId())) {
            get("projects", write.projectId(), false);
        }

        var id = UUID.randomUUID();
        var resourceKind = ResourceKindHandler.resolve(kind);
        validateModelPolicy(kind, write);
        var entity = converter.toEntity(resourceKind, id, IdentitySecurity.owner(), write);
        persistenceHandlers.resolve(resourceKind).save(entity);
        audit("CREATE_" + kind, id);
        return get(kind, id, false);
    }

    @Transactional
    @Override
    public ResourceResponseRecord update(String kind, UUID id, ResourceWriteRecord<Map<String, Object>> write) {
        var resourceKind = ResourceKindHandler.resolve(kind);
        var entity = findEntity(resourceKind, id, true);
        var old = converter.toRecord(entity);

        if (Objects.isNull(write.version()) || old.version() != write.version()) {
            throw new DomainException(DomainError.CONFLICT, "Resource version conflict");
        }

        if (!Objects.equals(old.projectId(), write.projectId())) {
            throw new IllegalArgumentException("Changing resource ownership or project is forbidden");
        }

        validationHandler.validate(kind, write.data());
        validateModelPolicy(kind, write);
        converter.updateEntity(entity, write);
        incrementRevision(entity);
        persistenceHandlers.resolve(resourceKind).save(entity);
        audit("UPDATE_" + kind, id);
        return get(kind, id, false);
    }

    @Transactional
    @Override
    public void delete(String kind, UUID id, long version) {
        var resourceKind = ResourceKindHandler.resolve(kind);
        var entity = findEntity(resourceKind, id, true);
        var old = converter.toRecord(entity);

        if (old.version() != version) {
            throw new DomainException(DomainError.CONFLICT, "Resource version conflict");
        }

        entity.setDeleted(true);
        incrementRevision(entity);
        persistenceHandlers.resolve(resourceKind).save(entity);
        audit("DELETE_" + kind, id);
    }

    @Override
    public void audit(String operation, UUID id) {
        var entry = new AuditEntry();
        entry.setId(UUID.randomUUID());
        entry.setOwnerId(IdentitySecurity.owner());
        entry.setOperation(operation);
        entry.setResourceId(id);
        auditRepository.save(entry);
    }

    private void validateModelPolicy(String kind, ResourceWriteRecord<Map<String, Object>> write) {
        if ("model-policies".equals(kind)) {
            models.requireAvailableByModelIdAndEffortAndProjectId((String) write.data().get("modelId"), (String) write.data().get("effort"), write.projectId());
        }
    }

    private VersionedResource findEntity(ResourceKind kind, UUID id, boolean lock) {
        if (lock) {
            return resourceDAO.findLockedByOwnerIdAndId(kind, IdentitySecurity.owner(), id).orElseThrow(() -> new DomainException(DomainError.NOT_FOUND));
        }

        return persistenceHandlers.resolve(kind).findByOwnerIdAndId(IdentitySecurity.owner(), id).orElseThrow(() -> new DomainException(DomainError.NOT_FOUND));
    }

    private void incrementRevision(VersionedResource entity) {
        entity.setVersion(entity.getVersion() + 1);
        entity.setUpdatedAt(Instant.now());
    }
}

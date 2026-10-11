package io.agentengine.core.common.service;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.common.security.IdentitySecurity;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private static final Set<String> KINDS = Set.of("projects", "profiles", "rules", "skills", "models", "model-policies", "memories");

    private final ResourceService resources;

    public KnowledgeServiceImpl(ResourceService resources) {
        this.resources = resources;
    }

    @Override
    public List<ResourceResponseRecord> list(String kind, UUID projectId, String query, int limit, int offset, boolean tombstones) {
        requireKind(kind);
        return resources.list(kind, projectId, query, limit, offset, tombstones);
    }

    @Override
    public ResourceResponseRecord get(String kind, UUID id) {
        requireKind(kind);
        return resources.get(kind, id, false);
    }

    @Override
    @Transactional
    public ResourceResponseRecord create(String kind, ResourceWriteRecord<Map<String, Object>> write) {
        requireWritable(kind);
        return resources.create(kind, write);
    }

    @Override
    @Transactional
    public ResourceResponseRecord update(String kind, UUID id, ResourceWriteRecord<Map<String, Object>> write) {
        requireWritable(kind);
        return resources.update(kind, id, write);
    }

    @Override
    @Transactional
    public void delete(String kind, UUID id, long version) {
        requireWritable(kind);
        resources.delete(kind, id, version);
    }

    private void requireWritable(String kind) {
        requireKind(kind);
        IdentitySecurity.knowledgeWrite();
    }

    private void requireKind(String kind) {
        if (!KINDS.contains(kind)) {
            throw new IllegalArgumentException("Resource is outside knowledge management");
        }
    }
}

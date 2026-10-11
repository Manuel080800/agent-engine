package io.agentengine.core.common.service;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;

import java.util.*;

public interface KnowledgeService {
    List<ResourceResponseRecord> list(String kind, UUID projectId, String query, int limit, int offset, boolean tombstones);

    ResourceResponseRecord get(String kind, UUID id);

    ResourceResponseRecord create(String kind, ResourceWriteRecord<Map<String, Object>> write);

    ResourceResponseRecord update(String kind, UUID id, ResourceWriteRecord<Map<String, Object>> write);

    void delete(String kind, UUID id, long version);
}

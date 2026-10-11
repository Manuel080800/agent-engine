package io.agentengine.core.common.service;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;

import java.util.*;

public interface ResourceService {
    String encode(Object value);

    List<ResourceResponseRecord> list(String kind, UUID project, String query, int limit, int offset, boolean tombstones);

    List<ResourceResponseRecord> all(String kind, UUID project);

    ResourceResponseRecord get(String kind, UUID id, boolean lock);

    ResourceResponseRecord create(String kind, ResourceWriteRecord<Map<String, Object>> write);

    ResourceResponseRecord update(String kind, UUID id, ResourceWriteRecord<Map<String, Object>> write);

    void delete(String kind, UUID id, long version);

    void audit(String operation, UUID id);
}

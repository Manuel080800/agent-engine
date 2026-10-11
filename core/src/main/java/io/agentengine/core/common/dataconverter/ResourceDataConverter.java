package io.agentengine.core.common.dataconverter;

import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.factory.ResourceEntityFactory;
import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ResourceDataConverter {
    public ResourceResponseRecord toRecord(VersionedResource entity) {
        return new ResourceResponseRecord(entity.getId(), entity.getProjectId(), entity.getVersion(), entity.isDeleted(), new LinkedHashMap<>(entity.getData()));
    }

    public List<ResourceResponseRecord> toRecords(List<? extends VersionedResource> entities) {
        return entities.stream().map(this::toRecord).toList();
    }

    public VersionedResource toEntity(ResourceKind kind, UUID id, UUID ownerId, ResourceWriteRecord<Map<String, Object>> request) {
        var entity = ResourceEntityFactory.create(kind);
        entity.setId(id);
        entity.setOwnerId(ownerId);
        entity.setProjectId(request.projectId());
        entity.setData(new LinkedHashMap<>(request.data()));
        return entity;
    }

    public void updateEntity(VersionedResource entity, ResourceWriteRecord<Map<String, Object>> request) {
        entity.setData(new LinkedHashMap<>(request.data()));
    }
}

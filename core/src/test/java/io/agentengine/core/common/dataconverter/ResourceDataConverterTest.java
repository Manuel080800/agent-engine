package io.agentengine.core.common.dataconverter;

import static org.junit.jupiter.api.Assertions.*;

import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.project.domain.Project;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResourceDataConverterTest {
    private final ResourceDataConverter converter = new ResourceDataConverter();

    @Test
    void writeConversionKeepsScalarOwnershipAndCopiesPayload() {
        // La entidad conserva identificadores escalares y no comparte el mapa mutable
        // de entrada.
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Map<String, Object> data = new HashMap<>(Map.of("name", "Original"));
        var dto = new ResourceWriteRecord<Map<String, Object>>(projectId, null, data);
        var entity = converter.toEntity(ResourceKind.PROJECT, id, ownerId, dto);
        data.put("name", "Mutated");
        assertInstanceOf(Project.class, entity);
        assertEquals(ownerId, entity.getOwnerId());
        assertEquals(projectId, entity.getProjectId());
        assertEquals("Original", entity.getData().get("name"));
    }

    @Test
    void responseConversionPreservesPublicContractWithoutMutatingEntity() {
        // El DTO mantiene revisión y tombstone; cambiar su mapa no altera la entidad.
        Project entity = new Project();
        entity.setId(UUID.randomUUID());
        entity.setProjectId(UUID.randomUUID());
        entity.setVersion(4);
        entity.setDeleted(true);
        entity.setData(new HashMap<>(Map.of("name", "Original")));
        var dto = converter.toRecord(entity);
        dto.data().put("name", "Mutated");
        assertEquals(entity.getId(), dto.id());
        assertEquals(entity.getProjectId(), dto.projectId());
        assertEquals(4, dto.version());
        assertTrue(dto.deleted());
        assertEquals("Original", entity.getData().get("name"));
    }
}

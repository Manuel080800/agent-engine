package io.agentengine.core.mcp.dataconverter;

import io.agentengine.core.common.record.ResourceWriteRecord;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class McpArgumentDataConverter {
    public UUID toUUID(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Identifier must be a UUID string");
        }

        return UUID.fromString(text);
    }

    public int toOffset(Object value) {
        var offset = Objects.isNull(value) ? 0 : toNonnegativeLong(value);

        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Offset exceeds the pagination limit");
        }

        return (int) offset;
    }

    public ResourceWriteRecord<Map<String, Object>> toWriteRecord(UUID projectId, Map<String, Object> arguments) {
        Long version = arguments.get("version") == null ? null : toNonnegativeLong(arguments.get("version"));
        var supplied = arguments.get("data");
        Map<String, Object> data = null;

        if (Objects.nonNull(supplied)) {
            if (!(supplied instanceof Map<?, ?> fields)) {
                throw new IllegalArgumentException("Resource data must be an object");
            }

            data = new LinkedHashMap<>();

            for (var entry : fields.entrySet()) {
                if (!(entry.getKey() instanceof String name)) {
                    throw new IllegalArgumentException("Resource field names must be strings");
                }

                data.put(name, entry.getValue());
            }
        }

        return new ResourceWriteRecord<>(projectId, version, data);
    }

    private long toNonnegativeLong(Object value) {
        if (!(value instanceof Number number) || number.longValue() < 0 || number.doubleValue() != number.longValue() || number.doubleValue() >= 0x1p63) {
            throw new IllegalArgumentException("Expected a nonnegative integer");
        }

        return number.longValue();
    }
}

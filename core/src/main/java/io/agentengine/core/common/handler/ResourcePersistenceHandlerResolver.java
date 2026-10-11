package io.agentengine.core.common.handler;

import io.agentengine.core.common.enums.ResourceKind;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResourcePersistenceHandlerResolver {
    private final Map<ResourceKind, ResourcePersistenceHandler> handlers = new EnumMap<>(ResourceKind.class);

    public ResourcePersistenceHandlerResolver(List<ResourcePersistenceHandler> handlers) {
        for (var handler : handlers) {
            if (this.handlers.put(handler.kind(), handler) != null) {
                throw new IllegalStateException("Duplicate resource persistence handler");
            }
        }

        if (this.handlers.size() != ResourceKind.values().length) {
            throw new IllegalStateException("Missing resource persistence handler");
        }
    }

    public ResourcePersistenceHandler resolve(ResourceKind kind) {
        return handlers.get(kind);
    }
}

package io.agentengine.webapp.common.handler;

import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.common.util.JsonCodec;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResourceRequestHandler {
    private final JsonCodec json;

    public ResourceRequestHandler(JsonCodec json) {
        this.json = json;
    }

    public ResourceWriteRecord<Map<String, Object>> convert(ResourceWriteRecord<?> request) {
        return new ResourceWriteRecord<>(request.projectId(), request.version(), json.fields(request.data()));
    }
}

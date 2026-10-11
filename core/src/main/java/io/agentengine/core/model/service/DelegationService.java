package io.agentengine.core.model.service;

import io.agentengine.core.common.record.*;
import io.agentengine.core.model.record.DelegationRequestRecord;

import java.util.*;

public interface DelegationService {
    Map<String, Object> start(DelegationRequestRecord request);

    ResourceResponseRecord finish(UUID taskId, UUID delegationId, long revision);
}

package io.agentengine.core.auth.service;

import io.agentengine.core.auth.record.LinkClientRequestRecord;
import io.agentengine.core.common.record.*;

import java.util.*;

public interface LocalClientService {
    ResourceResponseRecord link(LinkClientRequestRecord request);

    void revoke(UUID id, long version);

    void delete(UUID id, long version);
}

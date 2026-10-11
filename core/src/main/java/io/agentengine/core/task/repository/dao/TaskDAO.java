package io.agentengine.core.task.repository.dao;

import java.util.UUID;

public interface TaskDAO {
    int consumeConsent(UUID id, UUID owner, UUID task, UUID block, UUID delegation, UUID plan, long version, String model, String effort);

    long sequence(UUID task, String client, boolean lock);

    void synchronize(UUID task, String client, long sequence, long revision);
}

package io.agentengine.core.auth.service;

import io.agentengine.core.auth.record.LinkClientRequestRecord;
import io.agentengine.core.auth.repository.dao.AuthDAO;
import io.agentengine.core.common.record.*;
import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.service.ResourceService;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalClientServiceImpl implements LocalClientService {
    private final ResourceService resources;
    private final AuthDAO authDAO;

    public LocalClientServiceImpl(ResourceService resources, AuthDAO authDAO) {
        this.resources = resources;
        this.authDAO = authDAO;
    }

    @Transactional
    @Override
    public ResourceResponseRecord link(LinkClientRequestRecord request) {
        IdentitySecurity.human();

        if (!"agent-engine-local".equals(request.clientId())) {
            throw new IllegalArgumentException("Unknown local OAuth client");
        }

        if (resources.all("local-clients", null).stream().anyMatch(r -> request.clientId().equals(r.data().get("clientId")))) {
            throw new IllegalArgumentException("Client already linked");
        }

        return resources.create("local-clients", new ResourceWriteRecord<>(null, null, Map.of("name", request.name(), "clientId", request.clientId(), "enabled", true)));
    }

    @Transactional
    @Override
    public void revoke(UUID id, long version) {
        IdentitySecurity.human();
        var resource = resources.get("local-clients", id, false);
        resources.update("local-clients", id, new ResourceWriteRecord<>(null, version, Map.of("name", resource.data().get("name"), "clientId", resource.data().get("clientId"), "enabled", false)));
        authDAO.revokeAuthorizations(IdentitySecurity.owner(), resource.data().get("clientId").toString());
    }
}

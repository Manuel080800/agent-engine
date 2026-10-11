package io.agentengine.core.auth.service;

import io.agentengine.core.auth.record.OAuthRegistrationRequestRecord;

import java.util.*;

public interface OAuthClientService {
    void initializeLocalClient();

    Map<String, Object> resource();

    Map<String, Object> register(OAuthRegistrationRequestRecord registration);
}

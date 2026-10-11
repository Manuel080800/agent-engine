package io.agentengine.core.auth.service;

import io.agentengine.core.auth.record.RegistrationRequestRecord;

import java.util.*;

public interface AuthService {
    Map<String, Object> bootstrap(RegistrationRequestRecord request);

    Map<String, Object> invitation();

    Map<String, Object> redeem(RegistrationRequestRecord request);
}

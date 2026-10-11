package io.agentengine.core.user.dataconverter;

import io.agentengine.core.auth.record.RegistrationRequestRecord;
import io.agentengine.core.user.domain.UserAccount;
import io.agentengine.core.user.record.UserAccountRecord;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UserDataConverter {
    public UserAccountRecord toRecord(UserAccount entity) {
        return new UserAccountRecord(entity.getId(), entity.getEmail(), entity.isAdministrator(), entity.isEnabled(), entity.getCreatedAt());
    }

    public UserAccount toEntity(UUID id, RegistrationRequestRecord dto, String passwordHash, boolean administrator) {
        return new UserAccount(id, dto.email().toLowerCase(Locale.ROOT), passwordHash, administrator);
    }
}

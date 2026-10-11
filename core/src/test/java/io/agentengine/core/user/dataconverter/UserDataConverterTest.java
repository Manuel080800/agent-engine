package io.agentengine.core.user.dataconverter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.agentengine.core.auth.record.RegistrationRequestRecord;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserDataConverterTest {
    @Test
    void registrationAndResponseNeverExposePasswordMaterial() throws Exception {
        // El registro usa el hash calculado y el DTO público no expone contraseña, hash
        // ni token.
        var request = new RegistrationRequestRecord("Owner@Example.test", "plaintext-password", "invitation-token");
        UserDataConverter converter = new UserDataConverter();
        var entity = converter.toEntity(UUID.randomUUID(), request, "encoded-password", false);
        assertEquals("owner@example.test", entity.getEmail());
        assertEquals("encoded-password", entity.getPasswordHash());
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        String json = mapper.writeValueAsString(converter.toRecord(entity));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("invitation-token"));
        assertFalse(json.contains("encoded-password"));
    }
}

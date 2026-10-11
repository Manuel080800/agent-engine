package io.agentengine.core.common.handler;

import io.agentengine.core.common.util.JsonCodec;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceValidationHandlerTest {
    private final ResourceValidationHandler handler = new ResourceValidationHandler(new JsonCodec(JsonMapper.builder().build()));

    @Test
    void rejectsMissingNullAndInvalidRuleScopesWithActionableError() {
        // Un ámbito ausente o inválido produce un error de validación que MCP puede devolver sin HTTP 500.
        var data = new LinkedHashMap<String, Object>(Map.of("name", "Rule", "text", "Preserve behavior"));
        assertRuleScopeError(data);

        for (var scope : new Object[] {null, "Repositorio", 42}) {
            data.put("scope", scope);
            assertRuleScopeError(data);
        }
    }

    @Test
    void acceptsRepositoryRuleAndStillRequiresText() {
        // Los campos correctos permiten el alta; un contenido vacío sigue siendo inválido.
        assertDoesNotThrow(() -> handler.validate("rules", Map.of("name", "Rule", "text", "Preserve behavior", "scope", "repository")));
        assertThrows(IllegalArgumentException.class, () -> handler.validate("rules", Map.of("scope", "repository", "text", "")));
    }

    @Test
    void rejectsMissingAndNullPolicyRolesWithoutNullPointerException() {
        // El rol de política también se valida cuando falta o llega explícitamente como null.
        var data = new LinkedHashMap<String, Object>(Map.of("modelId", "configured-model", "effort", "medium"));
        assertThrows(IllegalArgumentException.class, () -> handler.validate("model-policies", data));
        data.put("role", null);
        assertThrows(IllegalArgumentException.class, () -> handler.validate("model-policies", data));
    }

    private void assertRuleScopeError(Map<String, Object> data) {
        var error = assertThrows(IllegalArgumentException.class, () -> handler.validate("rules", data));
        assertTrue(error.getMessage().contains("scope required"));
        assertTrue(error.getMessage().contains("repository"));
    }
}

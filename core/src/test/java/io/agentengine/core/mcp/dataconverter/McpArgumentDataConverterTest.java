package io.agentengine.core.mcp.dataconverter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpArgumentDataConverterTest {
    private final McpArgumentDataConverter converter = new McpArgumentDataConverter();

    @Test
    void malformedJSONArgumentsAreRejectedWithoutUncheckedCasts() {
        // Los argumentos MCP de tipo incorrecto se rechazan antes de llegar a los
        // servicios de persistencia.
        assertThrows(IllegalArgumentException.class, () -> converter.toWriteRecord(null, Map.of("data", "invalid")));
        assertThrows(IllegalArgumentException.class, () -> converter.toWriteRecord(null, Map.of("version", "1")));
        assertThrows(IllegalArgumentException.class, () -> converter.toOffset(1.5));
        assertThrows(IllegalArgumentException.class, () -> converter.toOffset(-1));
        assertThrows(IllegalArgumentException.class, () -> converter.toUUID(42));
    }

    @Test
    void validArgumentsBecomeAnIndependentDTO() {
        // La conversión conserva revisión y campos válidos en una copia separada del
        // mapa de entrada.
        var data = new java.util.HashMap<String, Object>(Map.of("name", "Project"));
        var dto = converter.toWriteRecord(null, Map.of("version", 2, "data", data));
        data.put("name", "Changed");
        assertEquals(2L, dto.version());
        assertEquals("Project", dto.data().get("name"));
        assertEquals(0, converter.toOffset(null));
    }
}

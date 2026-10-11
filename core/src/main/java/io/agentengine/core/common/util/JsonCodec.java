package io.agentengine.core.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonCodec {
    private final ObjectMapper mapper;

    public JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid JSON", ex);
        }
    }

    public Map<String, Object> decode(String value) {
        try {
            return mapper.readValue(value, new TypeReference<>() {
            });
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid stored JSON", ex);
        }
    }

    public Map<String, Object> fields(Object value) {
        var result = mapper.convertValue(value, new TypeReference<Map<String, Object>>() {
        });
        result.values().removeIf(java.util.Objects::isNull);
        return result;
    }
}

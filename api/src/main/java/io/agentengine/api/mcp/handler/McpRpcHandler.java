package io.agentengine.api.mcp.handler;

import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.util.JsonCodec;
import io.agentengine.core.mcp.record.RpcRequestRecord;
import io.agentengine.core.mcp.service.McpToolService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;

@Component
public class McpRpcHandler {
    private final McpToolService tools;
    private final JsonCodec json;
    private final String issuer;

    public McpRpcHandler(McpToolService tools, JsonCodec json, @Value("${engine.issuer}") String issuer) {
        this.tools = tools;
        this.json = json;
        this.issuer = issuer;
    }

    public ResponseEntity<?> rpc(RpcRequestRecord request, String origin, String protocol) {
        if (origin != null && (!Objects.equals(URI.create(origin).getScheme(), URI.create(issuer).getScheme()) || !Objects.equals(URI.create(origin).getRawAuthority(), URI.create(issuer).getRawAuthority()) || URI.create(origin).getQuery() != null || URI.create(origin).getFragment() != null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origin rejected");
        }

        if (!(IdentitySecurity.authentication() instanceof JwtAuthenticationToken) || IdentitySecurity.authentication().getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("SCOPE_context:read"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MCP bearer authorization required");
        }

        if (!"2.0".equals(request.jsonrpc()) || request.method() == null || (request.id() != null && !(request.id() instanceof String || request.id() instanceof Number))) {
            throw new IllegalArgumentException("Invalid JSON-RPC envelope");
        }

        if (protocol != null && !Set.of("2025-03-26", "2025-06-18", "2025-11-25").contains(protocol)) {
            throw new IllegalArgumentException("Unsupported MCP version");
        }

        if (request.id() == null) {
            return ResponseEntity.accepted().build();
        }

        Object result;

        switch (request.method()) {
            case "initialize" -> {
                var version = Objects.toString(request.params() == null ? null : request.params().get("protocolVersion"), "2025-11-25");
                result = Map.of("protocolVersion", Set.of("2025-03-26", "2025-06-18", "2025-11-25").contains(version) ? version : "2025-11-25", "capabilities", Map.of("tools", Map.of("listChanged", false)), "serverInfo", Map.of("name", "agent-engine", "version", "1.0.0"));
            }

            case "ping" -> result = Map.of();
            case "tools/list" -> result = Map.of("tools", tools.definitions());
            case "tools/call" -> {
                try {
                    var params = request.params();

                    if (params == null || !(params.get("arguments") instanceof Map<?, ?> supplied)) {
                        throw new IllegalArgumentException("Tool arguments must be an object");
                    }

                    var arguments = new LinkedHashMap<String, Object>();

                    for (var entry : supplied.entrySet()) {
                        if (!(entry.getKey() instanceof String key)) {
                            throw new IllegalArgumentException("Tool argument names must be strings");
                        }

                        arguments.put(key, entry.getValue());
                    }

                    var value = tools.call(Objects.toString(params.get("name"), ""), arguments);
                    result = Map.of("content", List.of(Map.of("type", "text", "text", json.encode(value))), "isError", false);
                } catch (IllegalArgumentException | DomainException ex) {
                    result = Map.of("content", List.of(Map.of("type", "text", "text", ex.getMessage())), "isError", true);
                }
            }

            default -> {
                return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", request.id(), "error", Map.of("code", -32601, "message", "Method not found")));
            }
        }

        return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", request.id(), "result", result));
    }

}

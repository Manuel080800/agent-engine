package io.agentengine.api.mcp.controller;

import io.agentengine.api.mcp.handler.McpRpcHandler;
import io.agentengine.core.mcp.record.RpcRequestRecord;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mcp")
public class McpController {
    private final McpRpcHandler handler;

    public McpController(McpRpcHandler handler) {
        this.handler = handler;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rpc(@RequestBody RpcRequestRecord request, @RequestHeader(value = "Origin", required = false) String origin, @RequestHeader(value = "MCP-Protocol-Version", required = false) String protocol) {
        return handler.rpc(request, origin, protocol);
    }

    @GetMapping
    public ResponseEntity<Void> events() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}

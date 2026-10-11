package io.agentengine.core.mcp.record;

import java.util.Map;

public record RpcRequestRecord(String jsonrpc, Object id, String method, Map<String, Object> params) {
}

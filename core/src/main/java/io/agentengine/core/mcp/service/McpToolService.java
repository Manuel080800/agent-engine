package io.agentengine.core.mcp.service;

import java.util.*;

public interface McpToolService {
    List<Map<String, Object>> definitions();

    Object call(String name, Map<String, Object> args);
}

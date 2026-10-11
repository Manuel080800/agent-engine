package io.agentengine.core.mcp.service;

import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.core.common.service.ResourceService;
import io.agentengine.core.mcp.dataconverter.McpArgumentDataConverter;
import io.agentengine.core.task.service.TaskService;

import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class McpToolServiceImpl implements McpToolService {
    private static final Set<String> KNOWLEDGE = Set.of("projects", "profiles", "rules", "skills", "models", "model-policies", "memories");

    private final ResourceService resources;
    private final TaskService tasks;
    private final KnowledgeService knowledge;
    private final McpArgumentDataConverter converter;

    public McpToolServiceImpl(ResourceService resources, TaskService tasks, KnowledgeService knowledge, McpArgumentDataConverter converter) {
        this.resources = resources;
        this.tasks = tasks;
        this.knowledge = knowledge;
        this.converter = converter;
    }

    @Override
    public List<Map<String, Object>> definitions() {
        var result = new ArrayList<Map<String, Object>>();
        result.add(tool("get_context", "Read an owner and project scoped context snapshot", Map.of("taskId", Map.of("type", "string"), "projectId", Map.of("type", "string")), List.of("taskId"), true));
        result.add(tool("list_resources", "Search scoped knowledge and task resources", Map.of("kind", Map.of("type", "string"), "projectId", Map.of("type", "string"), "q", Map.of("type", "string"), "offset", Map.of("type", "integer", "minimum", 0)), List.of("kind"), true));
        var fields = Map.<String, Object>of("kind", Map.of("type", "string"), "projectId", Map.of("type", "string"), "id", Map.of("type", "string"), "version", Map.of("type", "integer"), "data", Map.of("type", "object", "description", "Complete resource fields. Rules require text and scope (user, personal-project, global, repository, skill or default); use repository for repository rules. Model policies require role, modelId and effort. Updates replace data, so include all fields to retain."));
        result.add(tool("create_resource", "Create administrative knowledge, task, immutable plan/block/finding or" + " pending memory proposal", fields, List.of("kind", "data"), false));
        result.add(tool("update_knowledge", "Update administrative knowledge with a version precondition", fields, List.of("kind", "id", "version", "data"), false));
        result.add(tool("delete_knowledge", "Tombstone administrative knowledge with a version precondition", fields, List.of("kind", "id", "version"), false));
        return result;
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required, boolean read) {
        return Map.of("name", name, "description", description, "inputSchema", Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false), "annotations", Map.of("readOnlyHint", read, "destructiveHint", !read, "openWorldHint", false));
    }

    private UUID uuid(Object value) {
        return converter.toUUID(value);
    }

    @Override
    public Object call(String name, Map<String, Object> args) {
        if (Objects.isNull(args)) {
            throw new IllegalArgumentException("Tool arguments required");
        }

        if (name.equals("get_context")) {
            return tasks.context(uuid(args.get("projectId")), uuid(args.get("taskId")));
        }

        var kind = Objects.toString(args.get("kind"), "");
        var project = uuid(args.get("projectId"));

        if (name.equals("list_resources")) {
            return resources.list(kind, project, Objects.toString(args.get("q"), ""), 50, converter.toOffset(args.get("offset")), false);
        }

        IdentitySecurity.knowledgeWrite();
        var write = converter.toWriteRecord(project, args);

        if (name.equals("create_resource")) {
            if (kind.equals("tasks")) {
                return tasks.create(write);
            }

            if (kind.equals("memory-proposals")) {
                return tasks.propose(write);
            }

            if (Set.of("plans", "blocks", "reviews").contains(kind)) {
                return tasks.createChild(kind, write);
            }

            if (KNOWLEDGE.contains(kind)) {
                return knowledge.create(kind, write);
            }
        } else if (KNOWLEDGE.contains(kind)) {
            if (name.equals("update_knowledge")) {
                return knowledge.update(kind, uuid(args.get("id")), write);
            }

            if (name.equals("delete_knowledge") && Objects.nonNull(write.version())) {
                knowledge.delete(kind, uuid(args.get("id")), write.version());
                return Map.of("deleted", true);
            }
        }

        throw new IllegalArgumentException("Unknown or restricted tool operation");
    }
}

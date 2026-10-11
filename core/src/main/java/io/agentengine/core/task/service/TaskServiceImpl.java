package io.agentengine.core.task.service;

import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.service.ResourceService;
import io.agentengine.core.common.util.HashUtils;
import io.agentengine.core.execution.dataconverter.ExecutionDataConverter;
import io.agentengine.core.execution.domain.ExecutionEvent;
import io.agentengine.core.execution.record.*;
import io.agentengine.core.execution.repository.ExecutionEventRepository;
import io.agentengine.core.execution.repository.ExecutionEvidenceRepository;
import io.agentengine.core.model.repository.ModelConsentRepository;
import io.agentengine.core.model.service.ModelConfigurationService;
import io.agentengine.core.rule.service.RuleResolver;
import io.agentengine.core.task.enums.TaskLevel;
import io.agentengine.core.task.enums.TaskState;
import io.agentengine.core.task.repository.ExecutionApprovalRepository;
import io.agentengine.core.task.repository.dao.TaskDAO;
import io.agentengine.core.task.workflow.CorrectionBudget;
import io.agentengine.core.task.workflow.WorkflowTransitions;

import java.time.Instant;
import java.util.*;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {
    private final ModelConfigurationService models;
    private final ResourceService resources;
    private final TaskDAO taskDAO;
    private final RuleResolver resolver;
    private final ExecutionApprovalRepository approvalRepository;
    private final ModelConsentRepository consentRepository;
    private final ExecutionEventRepository eventRepository;
    private final ExecutionEvidenceRepository evidenceRepository;
    private final ExecutionDataConverter converter;

    public TaskServiceImpl(ResourceService resources, TaskDAO taskDAO, RuleResolver resolver, ExecutionApprovalRepository approvalRepository, ModelConsentRepository consentRepository, ExecutionEventRepository eventRepository, ExecutionEvidenceRepository evidenceRepository, ExecutionDataConverter converter, ModelConfigurationService models) {
        this.models = models;
        this.resources = resources;
        this.taskDAO = taskDAO;
        this.resolver = resolver;
        this.approvalRepository = approvalRepository;
        this.consentRepository = consentRepository;
        this.eventRepository = eventRepository;
        this.evidenceRepository = evidenceRepository;
        this.converter = converter;
    }

    @Override
    public ResourceResponseRecord create(ResourceWriteRecord<Map<String, Object>> write) {
        IdentitySecurity.knowledgeWrite();
        var data = new HashMap<>(write.data());
        var level = TaskLevel.valueOf((String) data.getOrDefault("level", "Standard"));

        if (!(data.get("objective") instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Task objective required");
        }

        data.put("level", level.name());
        data.put("state", "RECEIVED");
        data.put("corrections", 0);
        data.put("finalRounds", 0);
        return resources.create("tasks", new ResourceWriteRecord<Map<String, Object>>(write.projectId(), null, data));
    }

    @Transactional
    @Override
    public ResourceResponseRecord createChild(String kind, ResourceWriteRecord<Map<String, Object>> write) {
        IdentitySecurity.knowledgeWrite();
        var data = new HashMap<>(write.data());
        var task = resources.get("tasks", reference(data, "taskId"), true);

        if (!Objects.equals(task.projectId(), write.projectId()) || "COMPLETED".equals(task.data().get("state"))) {
            throw new IllegalArgumentException("Invalid task relationship");
        }

        if (kind.equals("plans")) {
            if (Set.of("EXECUTING", "VERIFYING", "CORRECTING", "FINAL_REVIEW", "FINAL_CORRECTING").contains(task.data().get("state"))) {
                throw new IllegalArgumentException("Suspend the active task before registering a plan amendment");
            }

            if (!(data.get("original") instanceof String original) || original.isBlank() || !(data.get("scope") instanceof List<?> scope) || scope.isEmpty() || !(data.get("acceptanceCriteria") instanceof List<?> criteria) || criteria.isEmpty()) {
                throw new IllegalArgumentException("Original plan, scope and acceptance criteria required");
            }

            data.put("hash", HashUtils.sha256(original));
            var plan = resources.create(kind, new ResourceWriteRecord<Map<String, Object>>(write.projectId(), null, data));
            var updated = new HashMap<>(task.data());
            updated.put("skillsSnapshot", resources.all("skills", null).stream().filter(r -> Objects.isNull(r.projectId()) || Objects.equals(r.projectId(), write.projectId())).map(r -> Map.of("id", r.id().toString(), "version", r.version(), "data", r.data())).toList());
            updated.put("state", "PLANNED");
            updated.put("planId", plan.id().toString());
            updated.put("scopeHash", HashUtils.sha256(resources.encode(data.get("scope"))));
            updated.put("rulesHash", rulesHash(write.projectId()));

            if (data.containsKey("amendmentOf")) {
                var old = resources.get("plans", UUID.fromString(data.get("amendmentOf").toString()), false);

                if (!old.data().get("taskId").equals(task.id().toString())) {
                    throw new IllegalArgumentException("Invalid amendment source");
                }

                updated.put("state", "PLANNED");
            }

            resources.update("tasks", task.id(), new ResourceWriteRecord<Map<String, Object>>(task.projectId(), task.version(), updated));
            return plan;
        }

        if (kind.equals("blocks")) {
            if (!(data.get("scope") instanceof List<?> scope) || scope.isEmpty() || !(data.get("criteria") instanceof List<?> criteria) || criteria.isEmpty()) {
                throw new IllegalArgumentException("Block scope and criteria required");
            }

            var plan = resources.get("plans", reference(task.data(), "planId"), false);

            if (!((List<?>) plan.data().get("scope")).containsAll(scope)) {
                throw new IllegalArgumentException("Block outside plan scope");
            }

            data.put("state", "PENDING");
            data.put("corrections", 0);
        } else if (kind.equals("reviews")) {
            data.put("status", "OPEN");
            data.put("attempts", 0);

            if (!Set.of("BLOCKING", "WARNING").contains(data.get("severity"))) {
                throw new IllegalArgumentException("Finding severity required");
            }
        }

        return resources.create(kind, new ResourceWriteRecord<Map<String, Object>>(write.projectId(), null, data));
    }

    @Transactional
    @Override
    public ResourceResponseRecord propose(ResourceWriteRecord<Map<String, Object>> write) {
        IdentitySecurity.knowledgeWrite();
        var data = new HashMap<>(write.data());
        var task = resources.get("tasks", reference(data, "taskId"), false);

        if (!Objects.equals(task.projectId(), write.projectId())) {
            throw new IllegalArgumentException("Invalid proposal project");
        }

        data.put("status", "PENDING");
        return resources.create("memory-proposals", new ResourceWriteRecord<Map<String, Object>>(write.projectId(), null, data));
    }

    @Override
    public Map<String, Object> context(UUID project, UUID taskId) {
        var task = resources.get("tasks", taskId, false);

        if (!Objects.equals(project, task.projectId())) {
            throw new IllegalArgumentException("Project mismatch");
        }

        var rules = resources.all("rules", null).stream().filter(r -> Objects.isNull(r.projectId()) || Objects.equals(r.projectId(), project)).toList();
        rules = resolver.resolve(rules);
        var conflicts = new HashMap<String, Object>();

        for (var rule : rules) {
            var key = rule.data().getOrDefault("name", rule.id().toString()).toString() + ":" + rule.data().getOrDefault("scope", "global");

            if (Boolean.TRUE.equals(rule.data().get("required")) && !Boolean.FALSE.equals(rule.data().get("enabled"))) {
                var previous = conflicts.putIfAbsent(key, rule.data().get("text"));

                if (Objects.nonNull(previous) && !previous.equals(rule.data().get("text"))) {
                    throw new DomainException(DomainError.CONFLICT, "Mandatory rule conflict");
                }
            }
        }

        var result = new HashMap<String, Object>();
        result.put("userId", IdentitySecurity.owner());
        result.put("projectId", project);
        result.put("task", task);
        result.put("models", models.listByProjectId(project));
        result.put("rules", rules);
        result.put("requiredRules", rules.stream().filter(r -> Boolean.TRUE.equals(r.data().get("required"))).toList());
        result.put("rulesHash", rulesHash(project));
        result.put("skills", task.data().getOrDefault("skillsSnapshot", resources.all("skills", null).stream().filter(r -> Objects.isNull(r.projectId()) || Objects.equals(r.projectId(), project)).toList()));
        result.put("memories", resources.list("memories", null, "", 100, 0, false).stream().filter(r -> Objects.isNull(r.projectId()) || Objects.equals(r.projectId(), project)).limit(20).toList());
        result.put("expiresAt", rules.stream().filter(r -> r.data().get("expiresAt") != null).map(r -> Instant.parse(r.data().get("expiresAt").toString())).min(Comparator.naturalOrder()).filter(t -> t.isBefore(Instant.now().plusSeconds(4 * 3600))).orElse(Instant.now().plusSeconds(4 * 3600)));
        result.put("executionAuthorized", task.data().containsKey("planId") && approved(task) && Objects.equals(task.data().get("rulesHash"), rulesHash(project)));

        if (task.data().containsKey("planId")) {
            result.put("plan", resources.get("plans", reference(task.data(), "planId"), false));
        }

        result.put("blocks", children("blocks", task));
        result.put("checkpoints", children("checkpoints", task));

        if (IdentitySecurity.authentication() instanceof JwtAuthenticationToken bearer) {
            var client = bearer.getToken().getClaimAsString("client_id");
            result.put("localSequence", taskDAO.sequence(task.id(), client, false));
        }

        return result;
    }

    private List<ResourceResponseRecord> children(String kind, ResourceResponseRecord task) {
        return resources.all(kind, task.projectId()).stream().filter(r -> task.id().toString().equals(r.data().get("taskId"))).toList();
    }

    @Transactional
    @Override
    public UUID approve(UUID taskId, long version) {
        IdentitySecurity.human();
        var task = resources.get("tasks", taskId, true);

        if (task.version() != version || !Set.of("PLANNED", "WAITING_APPROVAL", "BLOCKED").contains(task.data().get("state"))) {
            throw new DomainException(DomainError.CONFLICT, "Approval version/state mismatch");
        }

        var plan = resources.get("plans", reference(task.data(), "planId"), false);
        var id = UUID.randomUUID();
        persistApproval(id, IdentitySecurity.owner(), taskId, plan.id(), plan.version(), task.data().get("scopeHash"), rulesHash(task.projectId()));
        resources.audit("HUMAN_APPROVED_PLAN", taskId);
        return id;
    }

    private boolean approved(ResourceResponseRecord task) {
        if ("Quick".equals(task.data().get("level"))) {
            return true;
        }

        var plan = resources.get("plans", reference(task.data(), "planId"), false);
        return hasValidApproval(IdentitySecurity.owner(), task.id(), plan.id(), plan.version(), task.data().get("scopeHash"), rulesHash(task.projectId()));
    }

    @Transactional
    @Override
    public UUID consent(UUID taskId, UUID blockId, UUID delegationId, String model, String effort) {
        IdentitySecurity.human();
        var task = resources.get("tasks", taskId, true);
        var block = resources.get("blocks", blockId, false);

        if (!taskId.toString().equals(block.data().get("taskId")) || Objects.isNull(delegationId)) {
            throw new IllegalArgumentException("Invalid delegation relationship");
        }

        var configuration = models.requireAvailableByModelIdAndEffortAndProjectId(model, effort, task.projectId());

        if (!configuration.requiresVerification()) {
            throw new IllegalArgumentException("Configured model does not require human verification");
        }

        var plan = resources.get("plans", reference(task.data(), "planId"), false);
        var id = UUID.randomUUID();
        persistConsent(id, IdentitySecurity.owner(), taskId, blockId, delegationId, plan.id(), plan.version(), model, effort, Instant.now().plusSeconds(120));
        resources.audit("HUMAN_VERIFIED_MODEL", taskId);
        return id;
    }

    @Transactional
    @Override
    public ResourceResponseRecord transition(TransitionRecord event) {
        var client = IdentitySecurity.coordinator();

        if (Objects.isNull(event.eventId()) || Objects.isNull(event.taskId()) || event.localSequence() < 1 || Objects.isNull(event.nextAction()) || event.nextAction().length() > 1000) {
            throw new IllegalArgumentException("Event identity, sequence and next action required");
        }

        var clientRows = resources.all("local-clients", null).stream().filter(r -> client.equals(r.data().get("clientId"))).toList();

        if (clientRows.isEmpty() || clientRows.stream().anyMatch(r -> Boolean.FALSE.equals(r.data().get("enabled")))) {
            throw new DomainException(DomainError.FORBIDDEN, "Local client not linked or revoked");
        }

        var hash = HashUtils.sha256(resources.encode(event));
        var task = resources.get("tasks", event.taskId(), true);
        var prior = eventHashes(event.eventId(), IdentitySecurity.owner());

        if (!prior.isEmpty()) {
            if (!prior.getFirst().equals(hash)) {
                throw new DomainException(DomainError.CONFLICT, "Event identifier conflict");
            }

            return task;
        }

        if (task.version() != event.expectedRevision()) {
            throw new DomainException(DomainError.CONFLICT, "Task revision conflict");
        }

        var sequence = taskDAO.sequence(task.id(), client, true);

        if (event.localSequence() != sequence + 1) {
            throw new DomainException(DomainError.CONFLICT, "Event sequence conflict");
        }

        var from = TaskState.valueOf(task.data().get("state").toString());
        var to = TaskState.valueOf(event.to());
        WorkflowTransitions.requireTransition(from, to);
        var data = new HashMap<>(task.data());

        if (to == TaskState.EXECUTING || to == TaskState.CORRECTING || to == TaskState.FINAL_CORRECTING) {
            if (!approved(task) || !Objects.equals(task.data().get("rulesHash"), rulesHash(task.projectId()))) {
                throw new DomainException(DomainError.CONFLICT, "Current plan/rules approval required");
            }

            if (Objects.isNull(event.evidence()) || !event.evidence().backupReady()) {
                throw new IllegalArgumentException("Verified local backup required");
            }
        }

        ResourceResponseRecord block = null;

        if (Objects.nonNull(event.blockId())) {
            block = resources.get("blocks", event.blockId(), true);

            if (!task.id().toString().equals(block.data().get("taskId"))) {
                throw new IllegalArgumentException("Block does not belong to task");
            }
        }

        if (to == TaskState.CORRECTING || to == TaskState.FINAL_CORRECTING) {
            if (Objects.isNull(block)) {
                throw new IllegalArgumentException("Correction block required");
            }

            if (Objects.isNull(event.findingId())) {
                throw new IllegalArgumentException("Registered finding required for a correction");
            }

            var finding = resources.get("reviews", event.findingId(), true);

            if (!task.id().toString().equals(finding.data().get("taskId")) || !block.id().toString().equals(finding.data().get("blockId")) || !"OPEN".equals(finding.data().get("status"))) {
                throw new IllegalArgumentException("Finding is not open in this block");
            }

            new CorrectionBudget(count(finding.data(), "attempts"), count(block.data(), "corrections"), count(data, "corrections"), count(data, "finalRounds")).requireAnotherAttempt(to == TaskState.FINAL_CORRECTING);
            var findingData = new HashMap<>(finding.data());
            findingData.put("attempts", count(findingData, "attempts") + 1);
            resources.update("reviews", finding.id(), new ResourceWriteRecord<>(finding.projectId(), finding.version(), findingData));
            data.put("corrections", count(data, "corrections") + 1);

            if (to == TaskState.FINAL_CORRECTING) {
                data.put("finalRounds", count(data, "finalRounds") + 1);
            }

            var blockData = new HashMap<>(block.data());
            blockData.put("corrections", count(blockData, "corrections") + 1);
            block = resources.update("blocks", block.id(), new ResourceWriteRecord<Map<String, Object>>(block.projectId(), block.version(), blockData));
        }

        if (Objects.nonNull(event.delegationId())) {
            if (Objects.isNull(block) || !Set.of(TaskState.EXECUTING, TaskState.CORRECTING, TaskState.FINAL_CORRECTING).contains(to)) {
                throw new IllegalArgumentException("Delegation must start with an approved execution block");
            }

            if (data.get("activeDelegation") != null) {
                throw new DomainException(DomainError.CONFLICT, "Only one active delegation is permitted");
            }

            var configuration = models.requireAvailableByModelIdAndEffortAndProjectId(event.modelId(), event.effort(), task.projectId());

            if (configuration.requiresVerification()) {
                var plan = resources.get("plans", reference(task.data(), "planId"), false);
                var consumed = taskDAO.consumeConsent(event.consentId(), IdentitySecurity.owner(), task.id(), event.blockId(), event.delegationId(), plan.id(), plan.version(), event.modelId(), event.effort());

                if (consumed != 1) {
                    throw new DomainException(DomainError.FORBIDDEN, "Individual model verification unavailable");
                }
            }

            data.put("activeDelegation", event.delegationId().toString());
        }

        var evidence = event.evidence();

        if (Objects.nonNull(evidence)) {
            if (Objects.isNull(evidence.snapshotHash()) || !evidence.snapshotHash().matches("[a-f0-9]{64}") || !Set.of("BACKUP", "BLOCK_REVIEW", "FINAL_REVIEW", "PROCESS").contains(evidence.kind())) {
                throw new IllegalArgumentException("Typed evidence required");
            }

            if (Objects.nonNull(evidence.tests())) {
                for (var test : evidence.tests()) {
                    if (Objects.isNull(test.commandHash()) || !test.commandHash().matches("[a-f0-9]{64}") || Objects.isNull(test.outputHash()) || !test.outputHash().matches("[a-f0-9]{64}")) {
                        throw new IllegalArgumentException("Invalid process evidence");
                    }
                }
            }

            if (evidence.passed() && (Objects.isNull(evidence.tests()) || evidence.tests().isEmpty() || evidence.tests().stream().anyMatch(t -> t.exitCode() != 0 || t.timedOut()))) {
                throw new IllegalArgumentException("Successful instrumental tests required");
            }

            if ("BLOCK_REVIEW".equals(evidence.kind()) && evidence.passed()) {
                if (Objects.isNull(block)) {
                    throw new IllegalArgumentException("Reviewed block required");
                }

                var blockData = new HashMap<>(block.data());
                blockData.put("state", "VERIFIED");
                resources.update("blocks", block.id(), new ResourceWriteRecord<Map<String, Object>>(block.projectId(), block.version(), blockData));
                data.remove("activeDelegation");

                for (var finding : children("reviews", task)) {
                    if (block.id().toString().equals(finding.data().get("blockId")) && "OPEN".equals(finding.data().get("status"))) {
                        var findingData = new HashMap<>(finding.data());
                        findingData.put("status", "RESOLVED");
                        resources.update("reviews", finding.id(), new ResourceWriteRecord<>(finding.projectId(), finding.version(), findingData));
                    }
                }
            }
        }

        if (to == TaskState.FINAL_REVIEW || to == TaskState.COMPLETED) {
            var blocks = children("blocks", task);

            if (blocks.isEmpty() || blocks.stream().anyMatch(b -> !"VERIFIED".equals(b.data().get("state")))) {
                throw new IllegalArgumentException("All blocks must be verified");
            }
        }

        if (to == TaskState.COMPLETED) {
            if (Objects.isNull(evidence) || !"FINAL_REVIEW".equals(evidence.kind()) || !evidence.passed() || data.get("activeDelegation") != null || !approved(task) || !Objects.equals(data.get("rulesHash"), rulesHash(task.projectId()))) {
                throw new IllegalArgumentException("Final Review Gate not satisfied");
            }

            if (children("reviews", task).stream().anyMatch(r -> "BLOCKING".equals(r.data().get("severity")) && !"RESOLVED".equals(r.data().get("status")))) {
                throw new IllegalArgumentException("Open blocking findings");
            }
        }

        data.put("state", to.name());
        var result = resources.update("tasks", task.id(), new ResourceWriteRecord<Map<String, Object>>(task.projectId(), task.version(), data));
        persistEvent(IdentitySecurity.owner(), client, hash, event);
        taskDAO.synchronize(task.id(), client, event.localSequence(), result.version());
        resources.create("checkpoints", new ResourceWriteRecord<Map<String, Object>>(task.projectId(), null, new HashMap<>(Map.of("name", "Checkpoint " + event.localSequence(), "taskId", task.id().toString(), "state", to.name(), "nextAction", event.nextAction(), "planVersion", data.containsKey("planId") ? resources.get("plans", reference(data, "planId"), false).version() : 0))));
        return result;
    }

    @Transactional
    @Override
    public ResourceResponseRecord decideProposal(UUID id, long version, String decision, String editedText) {
        IdentitySecurity.human();
        var proposal = resources.get("memory-proposals", id, true);

        if (proposal.version() != version || !"PENDING".equals(proposal.data().get("status"))) {
            throw new DomainException(DomainError.CONFLICT, "Proposal already decided or stale");
        }

        var task = resources.get("tasks", reference(proposal.data(), "taskId"), false);

        if (!"COMPLETED".equals(task.data().get("state"))) {
            throw new DomainException(DomainError.CONFLICT, "Complete the originating task before deciding memory proposals");
        }

        var data = new HashMap<>(proposal.data());

        if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
            throw new IllegalArgumentException("Invalid decision");
        }

        if ("APPROVED".equals(decision)) {
            var text = Objects.isNull(editedText) ? Objects.toString(data.get("text"), "") : editedText;

            if (text.isBlank()) {
                throw new IllegalArgumentException("Memory text required");
            }

            if (resources.list("memories", proposal.projectId(), "", 100, 0, false).stream().anyMatch(m -> text.equals(m.data().get("text")))) {
                throw new DomainException(DomainError.CONFLICT, "Equivalent memory already exists");
            }

            resources.create("memories", new ResourceWriteRecord<Map<String, Object>>(proposal.projectId(), null, Map.of("name", data.getOrDefault("name", "Memory"), "text", text, "type", data.getOrDefault("type", "knowledge"), "source", data.get("taskId"), "trust", "HUMAN_APPROVED")));
        }

        data.put("status", decision);
        return resources.update("memory-proposals", id, new ResourceWriteRecord<Map<String, Object>>(proposal.projectId(), version, data));
    }

    private String rulesHash(UUID project) {
        var versions = new ArrayList<String>();
        resources.all("rules", null).stream().filter(r -> Objects.isNull(r.projectId()) || Objects.equals(r.projectId(), project)).sorted(Comparator.comparing(r -> r.id().toString())).forEach(r -> versions.add(r.id() + ":" + r.version()));
        return HashUtils.sha256(resources.encode(versions));
    }

    private void persistApproval(UUID id, UUID owner, UUID task, UUID plan, long version, Object scope, String rules) {
        approvalRepository.saveAndFlush(converter.toApprovalEntity(id, owner, task, plan, version, scope, rules));
    }

    private boolean hasValidApproval(UUID owner, UUID task, UUID plan, long version, Object scope, String rules) {
        return approvalRepository.existsByOwnerIdAndTaskIdAndPlanIdAndPlanVersionAndScopeHashAndRulesHashAndRevokedFalse(owner, task, plan, version, scope.toString(), rules);
    }

    private void persistConsent(UUID id, UUID owner, UUID task, UUID block, UUID delegation, UUID plan, long version, String model, String effort, Instant expires) {
        var entity = converter.toConsentEntity(id, owner, task, block, delegation, plan, version, model, effort, expires);
        consentRepository.saveAndFlush(entity);
    }

    private List<String> eventHashes(UUID id, UUID owner) {
        return eventRepository.findByIdAndOwnerId(id, owner).map(ExecutionEvent::getPayloadHash).stream().toList();
    }

    private void persistEvent(UUID owner, String client, String hash, TransitionRecord event) {
        eventRepository.saveAndFlush(converter.toEventEntity(owner, client, hash, event));

        if (Objects.nonNull(event.evidence())) {
            evidenceRepository.saveAndFlush(converter.toEvidenceEntity(owner, event));
        }
    }

    private static int count(Map<String, Object> data, String key) {
        return ((Number) data.getOrDefault(key, 0)).intValue();
    }

    private static UUID reference(Map<String, Object> data, String key) {
        if (!(data.get(key) instanceof String value)) {
            throw new IllegalArgumentException(key + " must be a resource identifier");
        }

        return UUID.fromString(value);
    }
}

package io.agentengine.core.model.service;

import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.model.domain.Model;
import io.agentengine.core.model.domain.ModelPolicy;
import io.agentengine.core.model.record.ModelConfigurationRecord;
import io.agentengine.core.model.record.ModelSelectionRecord;
import io.agentengine.core.model.repository.ModelRepository;
import io.agentengine.core.model.repository.ModelPolicyRepository;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import io.agentengine.core.model.dataconverter.ModelConfigurationDataConverter;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModelConfigurationServiceImpl implements ModelConfigurationService {
    private final ModelConfigurationDataConverter converter;
    private final ModelRepository modelRepository;
    private final ModelPolicyRepository policyRepository;

    public ModelConfigurationServiceImpl(ModelRepository modelRepository, ModelPolicyRepository policyRepository, ModelConfigurationDataConverter converter) {
        this.converter = converter;
        this.modelRepository = modelRepository;
        this.policyRepository = policyRepository;
    }

    @Override
    public List<ModelConfigurationRecord> listByProjectId(UUID projectId) {
        return buildEffectiveModelStream(projectId).filter(model -> Boolean.TRUE.equals(model.getData().get("enabled"))).map(converter::toRecord).sorted(Comparator.comparingInt(ModelConfigurationRecord::priority)).toList();
    }

    @Override
    public ModelConfigurationRecord requireAvailableByModelIdAndEffortAndProjectId(String modelId, String effort, UUID projectId) {
        var model = buildModelStream(projectId).filter(entity -> Objects.equals(modelId, entity.getData().get("modelId"))).findFirst().orElseThrow(() -> new IllegalArgumentException("Configured model unavailable"));
        var configuration = converter.toRecord(model);

        if (!Boolean.TRUE.equals(model.getData().get("enabled")) || !configuration.efforts().contains(effort)) {
            throw new IllegalArgumentException("Configured model/effort unavailable");
        }

        return configuration;
    }

    @Override
    public ModelSelectionRecord selectByRoleAndProjectId(String role, UUID projectId) {
        var policy = buildPolicyStream(projectId).filter(entity -> Objects.equals(role, entity.getData().get("role"))).findFirst();

        if (policy.isPresent()) {
            var data = policy.get().getData();
            var modelId = (String) data.get("modelId");
            var effort = (String) data.get("effort");
            var model = requireAvailableByModelIdAndEffortAndProjectId(modelId, effort, projectId);
            return new ModelSelectionRecord(model, effort);
        }

        var model = buildEffectiveModelStream(projectId).filter(entity -> Boolean.TRUE.equals(entity.getData().get("enabled"))).map(converter::toRecord).min(Comparator.comparingInt(ModelConfigurationRecord::priority)).orElseThrow(() -> new IllegalArgumentException("No enabled model configured"));
        return new ModelSelectionRecord(model, model.efforts().getFirst());
    }

    private Stream<Model> buildModelStream(UUID projectId) {
        return modelRepository.findByParams(IdentitySecurity.owner(), null, false, Pageable.unpaged()).stream().filter(entity -> inScope(entity, projectId)).sorted(scopeOrder());
    }

    private Stream<ModelPolicy> buildPolicyStream(UUID projectId) {
        return policyRepository.findByParams(IdentitySecurity.owner(), null, false, Pageable.unpaged()).stream().filter(entity -> inScope(entity, projectId)).sorted(scopeOrder());
    }

    private boolean inScope(VersionedResource entity, UUID projectId) {
        return Objects.isNull(entity.getProjectId()) || Objects.equals(entity.getProjectId(), projectId);
    }

    private <T extends VersionedResource> Comparator<T> scopeOrder() {
        return Comparator.comparing(entity -> Objects.isNull(entity.getProjectId()));
    }

    private Stream<Model> buildEffectiveModelStream(UUID projectId) {
        var models = new LinkedHashMap<String, Model>();
        buildModelStream(projectId).forEach(model -> models.putIfAbsent((String) model.getData().get("modelId"), model));
        return models.values().stream();
    }
}

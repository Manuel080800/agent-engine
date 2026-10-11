package io.agentengine.core.model.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.agentengine.core.model.dataconverter.ModelConfigurationDataConverter;
import io.agentengine.core.model.domain.Model;
import io.agentengine.core.model.domain.ModelPolicy;
import io.agentengine.core.model.repository.ModelRepository;
import io.agentengine.core.model.repository.ModelPolicyRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ModelConfigurationServiceTest {
    private final ModelRepository models = mock(ModelRepository.class);
    private final ModelPolicyRepository policies = mock(ModelPolicyRepository.class);
    private final ModelConfigurationService service = new ModelConfigurationServiceImpl(models, policies, new ModelConfigurationDataConverter());

    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ownerId.toString(), null, List.of()));
        when(policies.findByParams(eq(ownerId), isNull(), eq(false), any(Pageable.class))).thenReturn(List.of());
    }

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customModelAndEffortUseTheirConfiguredVerificationRequirement() {
        // Un modelo nuevo utiliza esfuerzos y verificación del catálogo, sin nombres
        // fijos.
        configure(model(null, "custom-engine", "extended", true, true, 1));
        var model = service.requireAvailableByModelIdAndEffortAndProjectId("custom-engine", "extended", null);
        assertTrue(model.requiresVerification());
        assertEquals(List.of("extended"), model.efforts());
        assertThrows(IllegalArgumentException.class, () -> service.requireAvailableByModelIdAndEffortAndProjectId("custom-engine", "high", null));
    }

    @Test
    void projectOverrideCannotFallBackToAnEnabledGlobalModel() {
        // Deshabilitar el modelo en el proyecto impide eludirlo mediante la
        // configuración global.
        var projectId = UUID.randomUUID();
        configure(model(null, "custom-engine", "extended", true, false, 1), model(projectId, "custom-engine", "extended", false, true, 1));
        assertThrows(IllegalArgumentException.class, () -> service.requireAvailableByModelIdAndEffortAndProjectId("custom-engine", "extended", projectId));
        assertThrows(IllegalArgumentException.class, () -> service.selectByRoleAndProjectId("executor", projectId));
    }

    @Test
    void modelsInOtherProjectsAreUnavailable() {
        // La selección respeta propietario y ámbito; no utiliza modelos de otro
        // proyecto.
        configure(model(UUID.randomUUID(), "private-engine", "extended", true, false, 1));
        assertThrows(IllegalArgumentException.class, () -> service.requireAvailableByModelIdAndEffortAndProjectId("private-engine", "extended", UUID.randomUUID()));
        verify(models).findByParams(eq(ownerId), isNull(), eq(false), any(Pageable.class));
    }

    @Test
    void projectPolicyOverridesGlobalPolicyAndUsesCatalogEfforts() {
        // La política del proyecto prevalece sobre la global y admite esfuerzos
        // configurados.
        var projectId = UUID.randomUUID();
        configure(model(null, "custom-engine", "extended", true, true, 1));
        var global = new ModelPolicy();
        global.setData(Map.of("role", "executor", "modelId", "missing-engine", "effort", "high"));
        var scoped = new ModelPolicy();
        scoped.setProjectId(projectId);
        scoped.setData(Map.of("role", "executor", "modelId", "custom-engine", "effort", "extended"));
        when(policies.findByParams(eq(ownerId), isNull(), eq(false), any(Pageable.class))).thenReturn(List.of(global, scoped));
        var selection = service.selectByRoleAndProjectId("executor", projectId);
        assertEquals("custom-engine", selection.model().modelId());
        assertEquals("extended", selection.effort());
        assertTrue(selection.model().requiresVerification());
    }

    @Test
    void fallbackUsesPriorityAndFirstConfiguredEffort() {
        // Sin política, la prioridad y el primer esfuerzo configurado determinan la
        // selección.
        configure(model(null, "secondary-engine", "slow", true, false, 5), model(null, "preferred-engine", "fast", true, false, 1));
        var selection = service.selectByRoleAndProjectId("planner", null);
        assertEquals("preferred-engine", selection.model().modelId());
        assertEquals("fast", selection.effort());
        assertFalse(selection.model().requiresVerification());
    }

    private void configure(Model... catalog) {
        when(models.findByParams(eq(ownerId), isNull(), eq(false), any(Pageable.class))).thenReturn(List.of(catalog));
    }

    private Model model(UUID projectId, String modelId, String effort, boolean enabled, boolean requiresVerification, int priority) {
        var model = new Model();
        model.setProjectId(projectId);
        model.setData(Map.of("modelId", modelId, "efforts", List.of(effort), "enabled", enabled, "requiresVerification", requiresVerification, "priority", priority));
        return model;
    }
}

package io.agentengine.core.model.dataconverter;

import io.agentengine.core.model.domain.Model;
import io.agentengine.core.model.record.ModelConfigurationRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ModelConfigurationDataConverter {
    public ModelConfigurationRecord toRecord(Model model) {
        var data = model.getData();

        if (!(data.get("efforts") instanceof List<?> efforts) || efforts.isEmpty() || efforts.stream().anyMatch(effort -> !(effort instanceof String text) || text.isBlank())) {
            throw new IllegalArgumentException("Configured model requires valid efforts");
        }

        var priority = data.get("priority") instanceof Number number ? number.intValue() : Integer.MAX_VALUE;
        var requiresVerification = Boolean.TRUE.equals(data.get("requiresVerification"));
        return new ModelConfigurationRecord((String) data.get("modelId"), efforts.stream().map(String.class::cast).toList(), priority, requiresVerification);
    }
}

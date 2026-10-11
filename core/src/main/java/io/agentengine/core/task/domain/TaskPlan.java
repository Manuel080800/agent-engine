package io.agentengine.core.task.domain;

import io.agentengine.core.common.domain.VersionedResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "taskplan")
public class TaskPlan extends VersionedResource {
    private static final long serialVersionUID = 1L;
}

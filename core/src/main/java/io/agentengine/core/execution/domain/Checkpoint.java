package io.agentengine.core.execution.domain;

import io.agentengine.core.common.domain.VersionedResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "checkpoint")
public class Checkpoint extends VersionedResource {
    private static final long serialVersionUID = 1L;
}

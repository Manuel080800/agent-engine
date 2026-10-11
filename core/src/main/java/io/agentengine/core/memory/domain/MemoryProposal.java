package io.agentengine.core.memory.domain;

import io.agentengine.core.common.domain.VersionedResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "memoryproposal")
public class MemoryProposal extends VersionedResource {
    private static final long serialVersionUID = 1L;
}

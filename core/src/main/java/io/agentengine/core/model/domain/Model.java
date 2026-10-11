package io.agentengine.core.model.domain;

import io.agentengine.core.common.domain.VersionedResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "model")
public class Model extends VersionedResource {
    private static final long serialVersionUID = 1L;
}

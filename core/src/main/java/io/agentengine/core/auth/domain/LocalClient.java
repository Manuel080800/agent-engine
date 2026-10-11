package io.agentengine.core.auth.domain;

import io.agentengine.core.common.domain.VersionedResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "localclient")
public class LocalClient extends VersionedResource {
    private static final long serialVersionUID = 1L;
}

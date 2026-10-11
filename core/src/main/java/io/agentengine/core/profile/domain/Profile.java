package io.agentengine.core.profile.domain;

import io.agentengine.core.common.domain.VersionedResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "profile")
public class Profile extends VersionedResource {
    private static final long serialVersionUID = 1L;
}

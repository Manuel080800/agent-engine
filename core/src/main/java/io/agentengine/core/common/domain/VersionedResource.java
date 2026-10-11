package io.agentengine.core.common.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class VersionedResource implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ownerid", nullable = false)
    private UUID ownerId;

    @Column(name = "projectid")
    private UUID projectId;

    @Column(name = "version", nullable = false)
    private long version = 1;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data = new LinkedHashMap<>();

    @Column(name = "updatedat", nullable = false)
    private Instant updatedAt = Instant.now();
}

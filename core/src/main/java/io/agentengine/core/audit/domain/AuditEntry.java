package io.agentengine.core.audit.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "administrativeaudit")
public class AuditEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ownerid", nullable = false)
    private UUID ownerId;

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "resourceid")
    private UUID resourceId;

    @Column(name = "createdat", nullable = false)
    private Instant createdAt = Instant.now();
}

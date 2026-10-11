package io.agentengine.core.task.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "executionapproval")
public class ExecutionApproval implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ownerid", nullable = false)
    private UUID ownerId;

    @Column(name = "taskid", nullable = false)
    private UUID taskId;

    @Column(name = "planid", nullable = false)
    private UUID planId;

    @Column(name = "planversion", nullable = false)
    private long planVersion;

    @Column(name = "scopehash", nullable = false, length = 64)
    private String scopeHash;

    @Column(name = "ruleshash", nullable = false, length = 64)
    private String rulesHash;

    @Column(name = "createdat", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

}

package io.agentengine.core.model.domain;

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
@Table(name = "modelconsent")
public class ModelConsent implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ownerid", nullable = false)
    private UUID ownerId;

    @Column(name = "taskid", nullable = false)
    private UUID taskId;

    @Column(name = "blockid", nullable = false)
    private UUID blockId;

    @Column(name = "delegationid", nullable = false)
    private UUID delegationId;

    @Column(name = "planid", nullable = false)
    private UUID planId;

    @Column(name = "planversion", nullable = false)
    private long planVersion;

    @Column(name = "modelid", nullable = false, length = 100)
    private String modelId;

    @Column(name = "effort", nullable = false, length = 30)
    private String effort;

    @Column(name = "expiresat", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumedat", nullable = true)
    private Instant consumedAt;

}

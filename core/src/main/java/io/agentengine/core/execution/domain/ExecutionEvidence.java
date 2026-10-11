package io.agentengine.core.execution.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "executionevidence")
public class ExecutionEvidence implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ownerid", nullable = false)
    private UUID ownerId;

    @Column(name = "taskid", nullable = false)
    private UUID taskId;

    @Column(name = "blockid", nullable = true)
    private UUID blockId;

    @Column(name = "eventid", nullable = false)
    private UUID eventId;

    @Column(name = "kind", nullable = false, length = 60)
    private String kind;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "createdat", nullable = false)
    private Instant createdAt = Instant.now();

}

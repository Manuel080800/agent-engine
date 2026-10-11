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
@Table(name = "executionevent")
public class ExecutionEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ownerid", nullable = false)
    private UUID ownerId;

    @Column(name = "taskid", nullable = false)
    private UUID taskId;

    @Column(name = "clientid", nullable = false, length = 200)
    private String clientId;

    @Column(name = "localsequence", nullable = false)
    private long localSequence;

    @Column(name = "eventtype", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payloadhash", nullable = false, length = 64)
    private String payloadHash;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "createdat", nullable = false)
    private Instant createdAt = Instant.now();

}

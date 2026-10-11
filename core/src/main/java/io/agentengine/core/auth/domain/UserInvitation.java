package io.agentengine.core.auth.domain;

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
@Table(name = "userinvitation")
public class UserInvitation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tokenhash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expiresat", nullable = false)
    private Instant expiresAt;

    @Column(name = "invitedbyid", nullable = false)
    private UUID invitedById;

    @Column(name = "consumedat", nullable = true)
    private Instant consumedAt;

}

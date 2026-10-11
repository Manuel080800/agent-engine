package io.agentengine.core.user.domain;

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
@Table(name = "useraccount")
public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "passwordhash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "administrator", nullable = false)
    private boolean administrator;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "createdat", nullable = false)
    private Instant createdAt = Instant.now();

    public UserAccount(UUID id, String email, String passwordHash, boolean administrator) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.administrator = administrator;
    }
}

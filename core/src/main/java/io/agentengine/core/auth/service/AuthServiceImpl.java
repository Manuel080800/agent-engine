package io.agentengine.core.auth.service;

import io.agentengine.core.auth.domain.UserInvitation;
import io.agentengine.core.auth.record.RegistrationRequestRecord;
import io.agentengine.core.auth.repository.UserInvitationRepository;
import io.agentengine.core.auth.repository.dao.AuthDAO;
import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.util.HashUtils;
import io.agentengine.core.user.dataconverter.UserDataConverter;
import io.agentengine.core.user.repository.UserRepository;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthDAO authDAO;
    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final UserDataConverter userConverter;
    private final PasswordEncoder passwords;
    private final String bootstrap;

    public AuthServiceImpl(AuthDAO authDAO, UserInvitationRepository invitationRepository, UserRepository userRepository, UserDataConverter userConverter, PasswordEncoder passwords, @Value("${engine.bootstrap-token}") String bootstrap) {
        this.authDAO = authDAO;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.userConverter = userConverter;
        this.passwords = passwords;
        this.bootstrap = bootstrap;
    }

    @Transactional
    @Override
    public Map<String, Object> bootstrap(RegistrationRequestRecord request) {
        var consumed = authDAO.lockBootstrapConsumed();
        var fresh = authDAO.isBootstrapFresh();

        if (consumed || !fresh || bootstrap.length() < 32 || !MessageDigest.isEqual(HashUtils.sha256(bootstrap).getBytes(), HashUtils.sha256(request.token()).getBytes())) {
            throw new DomainException(DomainError.FORBIDDEN, "Bootstrap unavailable");
        }

        var id = register(request, true);
        authDAO.consumeBootstrap();
        return Map.of("id", id);
    }

    @Transactional
    @Override
    public Map<String, Object> invitation() {
        IdentitySecurity.admin();
        var token = UUID.randomUUID() + "." + UUID.randomUUID();
        var id = UUID.randomUUID();
        var expiration = Instant.now().plusSeconds(86400);
        var invitation = new UserInvitation();
        invitation.setId(id);
        invitation.setTokenHash(HashUtils.sha256(token));
        invitation.setExpiresAt(expiration);
        invitation.setInvitedById(IdentitySecurity.owner());
        invitationRepository.saveAndFlush(invitation);
        return Map.of("id", id, "token", token, "expiresAt", expiration);
    }

    @Transactional
    @Override
    public Map<String, Object> redeem(RegistrationRequestRecord request) {
        var ids = authDAO.lockAvailableInvitation(HashUtils.sha256(request.token()));

        if (ids.isEmpty()) {
            throw new DomainException(DomainError.FORBIDDEN, "Invitation unavailable");
        }

        var id = register(request, false);
        authDAO.consumeInvitation(ids.getFirst());
        return Map.of("id", id);
    }

    private UUID register(RegistrationRequestRecord request, boolean admin) {
        var id = UUID.randomUUID();
        var passwordHash = passwords.encode(request.password());
        userRepository.saveAndFlush(userConverter.toEntity(id, request, passwordHash, admin));
        return id;
    }
}

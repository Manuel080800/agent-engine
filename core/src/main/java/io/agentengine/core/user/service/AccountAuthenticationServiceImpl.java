package io.agentengine.core.user.service;

import io.agentengine.core.user.domain.UserAccount;
import io.agentengine.core.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAuthenticationServiceImpl implements AccountAuthenticationService {
    private final UserRepository repository;

    public AccountAuthenticationServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identity) {
        var account = findEnabledAccount(identity).orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
        var username = account.getId().toString();
        var role = account.isAdministrator() ? "ADMIN" : "USER";
        return User.withUsername(username).password(account.getPasswordHash()).roles(role).build();
    }

    private Optional<UserAccount> findEnabledAccount(String identity) {
        UUID id;

        try {
            id = UUID.fromString(identity);
        } catch (IllegalArgumentException ex) {
            return repository.findByEmailAndEnabledTrue(identity);
        }

        return repository.findByIdAndEnabledTrue(id);
    }
}

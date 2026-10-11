package io.agentengine.core.auth.repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;

@Configuration
public class OAuthPersistenceConfiguration {
    @Bean
    RegisteredClientRepository clients(JdbcTemplate jdbc) {
        var repository = new JdbcRegisteredClientRepository(jdbc);
        return repository;
    }

    @Bean
    OAuth2AuthorizationService authorizations(JdbcTemplate jdbc, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationService(jdbc, clients);
    }

    @Bean
    OAuth2AuthorizationConsentService consents(JdbcTemplate jdbc, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
    }

}

package io.agentengine.api.auth.config;

import io.agentengine.api.auth.filter.RefreshSerializationFilter;
import io.agentengine.api.auth.handler.LocalClientAuthenticationHandler;

import io.agentengine.core.auth.service.TokenRefreshService;
import io.agentengine.core.auth.service.LocalClientAuthenticationService;
import io.agentengine.core.auth.service.AccessTokenService;
import io.agentengine.core.auth.handler.SigningKeyHandler;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.time.*;
import java.util.*;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwords() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthorizationServerSettings authorizationSettings(@Value("${engine.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    JWKSource<SecurityContext> keys(@Value("${engine.signing-key}") String location, SigningKeyHandler handler) throws Exception {
        var key = handler.loadOrCreate(location);
        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    @Bean
    OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> keys, @Value("${engine.issuer}") String issuer) {
        var jwt = new JwtGenerator(new NimbusJwtEncoder(keys));
        jwt.setJwtCustomizer(context -> {
            context.getClaims().audience(new ArrayList<>(List.of(issuer)));
            context.getClaims().claim("client_id", context.getRegisteredClient().getClientId());
        });
        OAuth2TokenGenerator<OAuth2RefreshToken> refresh = context -> {
            if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
                return null;
            }

            if (context.getRegisteredClient().getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE) && !context.getRegisteredClient().getClientId().equals("agent-engine-local")) {
                return null;
            }

            var now = Instant.now();
            return new OAuth2RefreshToken(UUID.randomUUID() + "." + UUID.randomUUID(), now, now.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive()));
        };
        return new DelegatingOAuth2TokenGenerator(jwt, new OAuth2AccessTokenGenerator(), refresh);
    }

    @Bean
    JwtDecoder decoder(JWKSource<SecurityContext> keys, AccessTokenService tokens, @Value("${engine.issuer}") String issuer) {
        var decoder = (NimbusJwtDecoder) org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration.jwtDecoder(keys);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), jwt -> {
            var valid = jwt.getAudience().contains(issuer) && tokens.isActive(jwt.getTokenValue());
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }));
        return decoder;
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "engine.authorization-server", havingValue = "true")
    SecurityFilterChain oauth(HttpSecurity http, LocalClientAuthenticationService localClients, @Value("${engine.issuer}") String issuer, TokenRefreshService refreshService) throws Exception {
        var config = OAuth2AuthorizationServerConfigurer.authorizationServer();
        http.securityMatcher(config.getEndpointsMatcher()).with(config, server -> server.clientAuthentication(client -> client.authenticationConverter(request -> {
            if (!"refresh_token".equals(request.getParameter("grant_type")) || !"agent-engine-local".equals(request.getParameter("client_id"))) {
                return null;
            }

            return new OAuth2ClientAuthenticationToken("agent-engine-local", ClientAuthenticationMethod.NONE, null, Map.of("grant_type", "refresh_token"));
        }).authenticationProvider(new LocalClientAuthenticationHandler(localClients))).authorizationServerMetadataEndpoint(metadata -> metadata.authorizationServerMetadataCustomizer(builder -> builder.claim("registration_endpoint", issuer + "/oauth/register"))).oidc(Customizer.withDefaults())).authorizeHttpRequests(a -> a.anyRequest().authenticated()).exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        http.addFilterBefore(new RefreshSerializationFilter(refreshService), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CookieCsrfTokenRepository csrfCookies() {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    @Order(2)
    SecurityFilterChain application(HttpSecurity http, @Value("${engine.route-prefix:/v1}") String prefix, @Value("${engine.issuer}") String issuer) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/error", "/login", prefix + "/auth/bootstrap", prefix + "/auth/redeem", "/actuator/health", "/.well-known/**", "/oauth/register").permitAll().anyRequest().authenticated()).oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()).authenticationEntryPoint((request, response, error) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + issuer.substring(0, issuer.length() - 4) + "/.well-known/oauth-protected-resource/api/mcp\"");
        })).csrf(csrf -> csrf.csrfTokenRepository(csrfCookies()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()).ignoringRequestMatchers(request -> request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Bearer "), request -> Set.of(prefix + "/auth/bootstrap", prefix + "/auth/redeem", "/oauth/register").contains(request.getServletPath()))).formLogin(form -> form.defaultSuccessUrl(java.net.URI.create(issuer).resolve("/").toString(), true)).logout(logout -> logout.logoutSuccessUrl(java.net.URI.create(issuer).resolve("/").toString()).invalidateHttpSession(true).deleteCookies("SESSION"));
        return http.build();
    }
}

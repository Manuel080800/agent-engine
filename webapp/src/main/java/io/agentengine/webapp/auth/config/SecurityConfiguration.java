package io.agentengine.webapp.auth.config;

import io.agentengine.core.auth.service.AccessTokenService;
import io.agentengine.core.auth.handler.SigningKeyHandler;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.*;
import org.springframework.security.web.csrf.*;

import java.util.*;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwords() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    JWKSource<SecurityContext> keys(@Value("${engine.signing-key}") String location, SigningKeyHandler handler) throws Exception {
        var key = handler.loadOrCreate(location);
        return new ImmutableJWKSet<>(new JWKSet(key));
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
    CookieCsrfTokenRepository csrfCookies() {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    @Order(1)
    SecurityFilterChain application(HttpSecurity http, @Value("${engine.route-prefix:/v1}") String prefix, @Value("${engine.issuer}") String issuer) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/error", "/assets/**", "/favicon.ico", "/login", prefix + "/auth/bootstrap", prefix + "/auth/redeem", prefix + "/auth/csrf", "/actuator/health", "/.well-known/**").permitAll().anyRequest().authenticated()).oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()).authenticationEntryPoint((request, response, error) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + issuer.substring(0, issuer.length() - 4) + "/.well-known/oauth-protected-resource/api/mcp\"");
        })).csrf(csrf -> csrf.csrfTokenRepository(csrfCookies()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()).ignoringRequestMatchers(request -> request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Bearer "), request -> Set.of(prefix + "/auth/bootstrap", prefix + "/auth/redeem").contains(request.getServletPath()))).exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) -> {
            if ("/login".equals(request.getServletPath())) {
                response.sendRedirect("/login?expired");
                return;
            }

            response.sendError(403);
        })).formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").failureUrl("/login?error").defaultSuccessUrl(java.net.URI.create(issuer).resolve("/").toString(), true)).logout(logout -> logout.logoutSuccessUrl(java.net.URI.create(issuer).resolve("/").toString()).invalidateHttpSession(true).deleteCookies("SESSION"));
        return http.build();
    }
}

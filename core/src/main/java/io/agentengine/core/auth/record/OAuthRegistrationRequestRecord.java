package io.agentengine.core.auth.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthRegistrationRequestRecord(List<String> redirect_uris, String client_name, String token_endpoint_auth_method, List<String> grant_types, String scope) {
}

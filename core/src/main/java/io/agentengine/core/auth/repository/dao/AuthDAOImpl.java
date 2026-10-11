package io.agentengine.core.auth.repository.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class AuthDAOImpl implements AuthDAO {
    private final NamedParameterJdbcTemplate jdbc;

    public AuthDAOImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean lockBootstrapConsumed() {
        var consumed = jdbc.queryForObject(buildBootstrapLockQuery(), new MapSqlParameterSource(), Boolean.class);
        return Boolean.TRUE.equals(consumed);
    }

    @Override
    public boolean isBootstrapFresh() {
        var fresh = jdbc.queryForObject(buildBootstrapFreshnessQuery(), new MapSqlParameterSource(), Boolean.class);
        return Boolean.TRUE.equals(fresh);
    }

    @Override
    public void consumeBootstrap() {
        jdbc.update(buildBootstrapConsumptionQuery(), new MapSqlParameterSource());
    }

    @Override
    public List<UUID> lockAvailableInvitation(String tokenHash) {
        return jdbc.queryForList(buildInvitationLockQuery(), buildInvitationTokenParameters(tokenHash), UUID.class);
    }

    @Override
    public void consumeInvitation(UUID id) {
        jdbc.update(buildInvitationConsumptionQuery(), buildInvitationIdParameters(id));
    }

    @Override
    public void revokeAuthorizations(UUID ownerId, String clientId) {
        jdbc.update(buildAuthorizationRevocationQuery(), buildAuthorizationParameters(ownerId, clientId));
    }

    @Override
    public boolean hasClientRelations(UUID ownerId, String clientId) {
        var parameters = buildAuthorizationParameters(ownerId, clientId);
        return Boolean.TRUE.equals(jdbc.queryForObject(buildClientRelationsQuery(), parameters, Boolean.class));
    }

    private String buildClientRelationsQuery() {
        return "SELECT EXISTS (SELECT 1 FROM executionevent WHERE ownerid = CAST(:ownerId AS uuid) AND clientid = :clientId) OR EXISTS (SELECT 1 FROM checkpointsync sync JOIN task ON task.id = sync.taskid WHERE task.ownerid = CAST(:ownerId AS uuid) AND sync.clientid = :clientId) OR EXISTS (SELECT 1 FROM oauth2_authorization auth JOIN oauth2_registered_client client ON client.id = auth.registered_client_id WHERE auth.principal_name = :ownerId AND client.client_id = :clientId) OR EXISTS (SELECT 1 FROM oauth2_authorization_consent consent JOIN oauth2_registered_client client ON client.id = consent.registered_client_id WHERE consent.principal_name = :ownerId AND client.client_id = :clientId)";
    }

    private String buildBootstrapLockQuery() {
        return "SELECT consumed FROM bootstrapstate WHERE id = 1 FOR UPDATE";
    }

    private String buildBootstrapFreshnessQuery() {
        return "SELECT createdat > now() - interval '15 minutes' FROM bootstrapstate WHERE id = 1";
    }

    private String buildBootstrapConsumptionQuery() {
        return "UPDATE bootstrapstate SET consumed = true WHERE id = 1";
    }

    private String buildInvitationLockQuery() {
        return "SELECT id FROM userinvitation WHERE tokenhash = :tokenHash AND consumedat IS NULL AND expiresat > now() FOR UPDATE";
    }

    private MapSqlParameterSource buildInvitationTokenParameters(String tokenHash) {
        return new MapSqlParameterSource("tokenHash", tokenHash);
    }

    private String buildInvitationConsumptionQuery() {
        return "UPDATE userinvitation SET consumedat = now() WHERE id = :id";
    }

    private MapSqlParameterSource buildInvitationIdParameters(UUID id) {
        return new MapSqlParameterSource("id", id);
    }

    private String buildAuthorizationRevocationQuery() {
        return "DELETE FROM oauth2_authorization WHERE principal_name = :ownerId AND registered_client_id IN ( SELECT id FROM oauth2_registered_client WHERE client_id = :clientId)";
    }

    private MapSqlParameterSource buildAuthorizationParameters(UUID ownerId, String clientId) {
        var parameters = new MapSqlParameterSource();
        parameters.addValue("ownerId", ownerId.toString());
        parameters.addValue("clientId", clientId);
        return parameters;
    }
}

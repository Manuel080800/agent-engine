package io.agentengine.core.task.repository.dao;

import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class TaskDAOImpl implements TaskDAO {
    private final NamedParameterJdbcTemplate jdbc;

    public TaskDAOImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int consumeConsent(UUID id, UUID owner, UUID task, UUID block, UUID delegation, UUID plan, long version, String model, String effort) {
        var parameters = buildConsentParameters(id, owner, task, block, delegation, plan, version, model, effort);
        return jdbc.update(buildConsentConsumptionQuery(), parameters);
    }

    @Override
    public long sequence(UUID task, String client, boolean lock) {
        var parameters = buildSequenceParameters(task, client);

        if (lock) {
            jdbc.update(buildSequenceCreationQuery(), parameters);
        }

        var rows = jdbc.queryForList(buildSequenceQuery(lock), parameters, Long.class);
        return rows.isEmpty() ? 0 : rows.getFirst();
    }

    @Override
    public void synchronize(UUID task, String client, long sequence, long revision) {
        var parameters = buildSynchronizationParameters(task, client, sequence, revision);
        jdbc.update(buildSynchronizationQuery(), parameters);
    }

    private String buildConsentConsumptionQuery() {
        return "UPDATE modelconsent SET consumedat = now() WHERE id = :id AND ownerid = :owner AND taskid = :task AND blockid = :block AND delegationid = :delegation AND planid = :plan AND planversion = :version AND modelid = :model AND effort = :effort AND consumedat IS NULL AND expiresat > now()";
    }

    private MapSqlParameterSource buildConsentParameters(UUID id, UUID owner, UUID task, UUID block, UUID delegation, UUID plan, long version, String model, String effort) {
        var parameters = new MapSqlParameterSource();
        parameters.addValue("id", id);
        parameters.addValue("owner", owner);
        parameters.addValue("task", task);
        parameters.addValue("block", block);
        parameters.addValue("delegation", delegation);
        parameters.addValue("plan", plan);
        parameters.addValue("version", version);
        parameters.addValue("model", model);
        parameters.addValue("effort", effort);
        return parameters;
    }

    private String buildSequenceCreationQuery() {
        return "INSERT INTO checkpointsync(taskid, clientid) VALUES (:task, :client) ON CONFLICT DO NOTHING";
    }

    private String buildSequenceQuery(boolean lock) {
        var sql = "SELECT lastsequence FROM checkpointsync WHERE taskid = :task AND clientid = :client";
        return lock ? sql + " FOR UPDATE" : sql;
    }

    private MapSqlParameterSource buildSequenceParameters(UUID task, String client) {
        var parameters = new MapSqlParameterSource();
        parameters.addValue("task", task);
        parameters.addValue("client", client);
        return parameters;
    }

    private String buildSynchronizationQuery() {
        return "UPDATE checkpointsync SET lastsequence = :sequence, remoterevision = :revision WHERE taskid = :task AND clientid = :client";
    }

    private MapSqlParameterSource buildSynchronizationParameters(UUID task, String client, long sequence, long revision) {
        var parameters = buildSequenceParameters(task, client);
        parameters.addValue("sequence", sequence);
        parameters.addValue("revision", revision);
        return parameters;
    }
}

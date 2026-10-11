package io.agentengine.core.common.repository.dao;

import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.factory.ResourceEntityFactory;
import io.agentengine.core.common.util.JsonCodec;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ResourceDAOImpl implements ResourceDAO {
    private final EntityManager entityManager;
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonCodec json;

    public ResourceDAOImpl(EntityManager entityManager, NamedParameterJdbcTemplate jdbc, JsonCodec json) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public List<VersionedResource> search(ResourceKind kind, UUID ownerId, UUID projectId, String query, int limit, int offset, boolean tombstones) {
        entityManager.flush();
        var sql = buildSearchQuery(kind);
        var parameters = buildSearchParameters(ownerId, projectId, query, limit, offset, tombstones);
        return jdbc.query(sql, parameters, buildRowMapper(kind));
    }

    @Override
    public Optional<VersionedResource> findLockedByOwnerIdAndId(ResourceKind kind, UUID ownerId, UUID id) {
        var query = buildLockedQuery(kind);
        buildLockParameters(ownerId, id).forEach(query::setParameter);
        var rows = query.getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        var resource = rows.getFirst();
        entityManager.refresh(resource, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(resource);
    }

    private String buildSearchQuery(ResourceKind kind) {
        var entityType = ResourceEntityFactory.create(kind).getClass();
        var table = entityType.getAnnotation(Table.class).name();
        return "SELECT * FROM %s WHERE ownerid = :ownerId AND (:tombstones OR NOT deleted) AND (CAST(:projectId AS uuid) IS NULL OR projectid = CAST(:projectId AS uuid)) AND (:query = '' OR search @@ websearch_to_tsquery('simple', :query)) ORDER BY updatedat DESC, id LIMIT :limit OFFSET :offset".formatted(table);
    }

    private MapSqlParameterSource buildSearchParameters(UUID ownerId, UUID projectId, String query, int limit, int offset, boolean tombstones) {
        var parameters = new MapSqlParameterSource();
        parameters.addValue("ownerId", ownerId);
        parameters.addValue("projectId", projectId);
        parameters.addValue("query", query);
        parameters.addValue("limit", limit);
        parameters.addValue("offset", offset);
        parameters.addValue("tombstones", tombstones);
        return parameters;
    }

    private TypedQuery<? extends VersionedResource> buildLockedQuery(ResourceKind kind) {
        var entityType = ResourceEntityFactory.create(kind).getClass();
        var jpql = "SELECT resource FROM " + entityType.getSimpleName() + " resource WHERE resource.id = :id AND resource.ownerId = :ownerId AND NOT resource.deleted";
        return entityManager.createQuery(jpql, entityType).setLockMode(LockModeType.PESSIMISTIC_WRITE);
    }

    private Map<String, Object> buildLockParameters(UUID ownerId, UUID id) {
        return Map.of("id", id, "ownerId", ownerId);
    }

    private RowMapper<VersionedResource> buildRowMapper(ResourceKind kind) {
        return (row, index) -> mapRow(kind, row);
    }

    private VersionedResource mapRow(ResourceKind kind, ResultSet row) throws SQLException {
        var entity = ResourceEntityFactory.create(kind);
        entity.setId(row.getObject("id", UUID.class));
        entity.setOwnerId(row.getObject("ownerid", UUID.class));
        entity.setProjectId(row.getObject("projectid", UUID.class));
        entity.setVersion(row.getLong("version"));
        entity.setDeleted(row.getBoolean("deleted"));
        entity.setUpdatedAt(row.getTimestamp("updatedat").toInstant());
        entity.setData(json.decode(row.getString("data")));
        return entity;
    }
}

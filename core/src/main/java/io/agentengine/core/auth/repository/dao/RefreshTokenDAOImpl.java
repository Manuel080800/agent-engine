package io.agentengine.core.auth.repository.dao;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class RefreshTokenDAOImpl implements RefreshTokenDAO {
    private final NamedParameterJdbcTemplate jdbc;

    public RefreshTokenDAOImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lockByTokenHash(String tokenHash) {
        jdbc.query(buildLockQuery(), buildLockParameters(tokenHash), result -> null);
    }

    private String buildLockQuery() {
        return "SELECT pg_advisory_xact_lock(hashtextextended(:tokenHash, 0))";
    }

    private Map<String, Object> buildLockParameters(String tokenHash) {
        return Map.of("tokenHash", tokenHash);
    }
}

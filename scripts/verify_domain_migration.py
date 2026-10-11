#!/usr/bin/env python3
"""Verify V1-V6 domain renames in a disposable schema and always roll back."""

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "core/src/main/resources/db/migration"


def verify():
    original = "\n".join(
        (MIGRATIONS / name).read_text()
        for name in ("V1__engine.sql", "V2__oauth.sql", "V3__sessions.sql")
    )
    verification = (MIGRATIONS / "V5__dynamic_model_verification.sql").read_text()
    defaults = (MIGRATIONS / "V6__model_defaults.sql").read_text()
    renamed = (MIGRATIONS / "V4__domain_naming.sql").read_text()
    sql = (
        "BEGIN;\nCREATE SCHEMA migrationcheck;\nSET LOCAL search_path = migrationcheck;\n"
        + original
    )
    sql += """
INSERT INTO users(id, email, password_hash) VALUES
    ('10000000-0000-0000-0000-000000000001', 'migration@example.test', 'test-only-hash');
INSERT INTO projects(id, owner_id, data) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '{"name":"Legacy project"}');
INSERT INTO model_role_policies(id, owner_id, project_id, data) VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', '{"role":"planner","modelId":"gpt-6-luna"}');
INSERT INTO model_catalog(id, owner_id, data) VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '{"requiresConsent":true}'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '{"requiresConsent":false}'),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '{"requiresConsent":true,"requiresVerification":false}');
INSERT INTO model_catalog(id, owner_id, deleted, data) VALUES
    ('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', true,
     '{"modelId":"gpt-6.1-sol","efforts":["custom"],"requiresConsent":true}');
""" + renamed + verification + defaults
    sql += """
-- Verify that future accounts inherit configured SQL defaults, including new templates.
INSERT INTO modeldefault(modelid, data) VALUES ('custom-default', '{"modelId":"custom-default","efforts":["custom"],"enabled":true,"requiresVerification":true}');
INSERT INTO useraccount(id, email, passwordhash) VALUES ('10000000-0000-0000-0000-000000000002', 'new-account@example.test', 'test-only-hash');
DO $$
DECLARE blocked boolean := false;
BEGIN
    IF (SELECT count(*) FROM model WHERE ownerid = '10000000-0000-0000-0000-000000000001' AND data->>'modelId' = 'gpt-6.1-sol') <> 1
        OR NOT EXISTS (SELECT 1 FROM model WHERE id = '40000000-0000-0000-0000-000000000004'
            AND deleted AND data->'efforts' = '["custom"]'::jsonb) THEN
        RAISE EXCEPTION 'SQL defaults overwrote a customization or restored a tombstone';
    END IF;
    IF (SELECT count(*) FROM model WHERE ownerid = '10000000-0000-0000-0000-000000000002') <> 3
        OR NOT EXISTS (SELECT 1 FROM model WHERE ownerid = '10000000-0000-0000-0000-000000000002'
            AND data->>'modelId' = 'custom-default' AND data->>'requiresVerification' = 'true') THEN
        RAISE EXCEPTION 'New account did not receive dynamic SQL model defaults';
    END IF;
    IF (SELECT data->>'requiresVerification' FROM model WHERE id = '40000000-0000-0000-0000-000000000001') <> 'true'
        OR (SELECT data->>'requiresVerification' FROM model WHERE id = '40000000-0000-0000-0000-000000000002') <> 'false'
        OR (SELECT data->>'requiresVerification' FROM model WHERE id = '40000000-0000-0000-0000-000000000003') <> 'false'
        OR EXISTS (SELECT 1 FROM model WHERE data ? 'requiresConsent') THEN
        RAISE EXCEPTION 'Model verification requirement changed during migration';
    END IF;
    IF (SELECT passwordhash FROM useraccount WHERE email = 'migration@example.test') <> 'test-only-hash' THEN
        RAISE EXCEPTION 'User data changed during migration';
    END IF;
    IF (SELECT count(*) FROM modelrolepolicie WHERE projectid = '20000000-0000-0000-0000-000000000001'
        AND data->>'role' = 'planner') <> 1 THEN
        RAISE EXCEPTION 'Policy relationship or payload was lost';
    END IF;
    IF (SELECT count(*) FROM project WHERE search @@ websearch_to_tsquery('simple', 'Legacy')) <> 1 THEN
        RAISE EXCEPTION 'Full-text search was lost';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'migrationcheck'
        AND table_name IN ('useraccount', 'project', 'modelrolepolicie') AND position('_' IN column_name) > 0) THEN
        RAISE EXCEPTION 'Legacy domain columns remain';
    END IF;
    BEGIN
        DELETE FROM useraccount WHERE id = '10000000-0000-0000-0000-000000000001';
    EXCEPTION WHEN foreign_key_violation THEN
        blocked := true;
    END;
    IF NOT blocked THEN
        RAISE EXCEPTION 'Foreign-key constraints were lost';
    END IF;
END $$;
ROLLBACK;
"""
    subprocess.run(
        [
            "docker",
            "compose",
            "-p",
            "agentengine-verify",
            "exec",
            "-T",
            "database",
            "psql",
            "-U",
            "agentengine",
            "-d",
            "agentengine",
            "-v",
            "ON_ERROR_STOP=1",
            "-q",
        ],
        cwd=ROOT,
        input=sql,
        text=True,
        check=True,
    )
    print(
        "PASS: V1-V6 preserve records, scalar foreign keys and full-text search; ROLLBACK completed"
    )


if __name__ == "__main__":
    verify()

CREATE TABLE users (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE, password_hash varchar(255) NOT NULL,
 administrator boolean NOT NULL DEFAULT false, enabled boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE user_invitations (
 id uuid PRIMARY KEY, token_hash varchar(64) UNIQUE NOT NULL, expires_at timestamptz NOT NULL,
 invited_by uuid NOT NULL REFERENCES users(id), consumed_at timestamptz
);
CREATE TABLE bootstrap_state (id integer PRIMARY KEY CHECK(id=1), consumed boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now());
INSERT INTO bootstrap_state(id) VALUES(1);

CREATE TABLE projects (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), project_id uuid,
 version bigint NOT NULL DEFAULT 1, deleted boolean NOT NULL DEFAULT false,
 data jsonb NOT NULL, updated_at timestamptz NOT NULL DEFAULT now(),
 search tsvector GENERATED ALWAYS AS(to_tsvector('simple',data::text)) STORED
);
DO $$ DECLARE kind text; BEGIN
 FOREACH kind IN ARRAY ARRAY['profiles','rules','skills','model_catalog','model_role_policies','memories','memory_proposals','tasks','task_plans','task_blocks','checkpoints','review_findings','local_clients'] LOOP
  EXECUTE format('CREATE TABLE %I (
   id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), project_id uuid REFERENCES projects(id),
   version bigint NOT NULL DEFAULT 1, deleted boolean NOT NULL DEFAULT false, data jsonb NOT NULL,
   updated_at timestamptz NOT NULL DEFAULT now(),
   search tsvector GENERATED ALWAYS AS(to_tsvector(''simple'',data::text)) STORED)',kind);
  EXECUTE format('CREATE INDEX ON %I(owner_id,project_id,deleted)',kind);
  EXECUTE format('CREATE INDEX ON %I USING GIN(search)',kind);
 END LOOP;
END $$;
CREATE TABLE execution_approvals (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), task_id uuid NOT NULL REFERENCES tasks(id),
 plan_id uuid NOT NULL REFERENCES task_plans(id), plan_version bigint NOT NULL, scope_hash varchar(64) NOT NULL, rules_hash varchar(64) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), revoked boolean NOT NULL DEFAULT false
);
CREATE TABLE model_consents (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), task_id uuid NOT NULL REFERENCES tasks(id),
 block_id uuid NOT NULL REFERENCES task_blocks(id), delegation_id uuid NOT NULL, plan_id uuid NOT NULL REFERENCES task_plans(id), plan_version bigint NOT NULL,
 model_id varchar(100) NOT NULL, effort varchar(30) NOT NULL, expires_at timestamptz NOT NULL,
 consumed_at timestamptz, UNIQUE(task_id,delegation_id)
);
CREATE TABLE execution_events (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), task_id uuid NOT NULL REFERENCES tasks(id),
 client_id varchar(200) NOT NULL, local_sequence bigint NOT NULL, event_type varchar(100) NOT NULL,
 payload_hash varchar(64) NOT NULL, data jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(task_id,client_id,local_sequence)
);
CREATE TABLE execution_evidence (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), task_id uuid NOT NULL REFERENCES tasks(id),
 block_id uuid REFERENCES task_blocks(id), event_id uuid NOT NULL REFERENCES execution_events(id),
 kind varchar(60) NOT NULL, data jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE checkpoint_sync (
 task_id uuid NOT NULL REFERENCES tasks(id), client_id varchar(200) NOT NULL,
 last_sequence bigint NOT NULL DEFAULT 0, remote_revision bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(task_id,client_id)
);
CREATE TABLE administrative_audit (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id), operation varchar(100) NOT NULL,
 resource_id uuid, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON execution_events(owner_id,task_id,created_at);
CREATE INDEX ON execution_evidence(owner_id,task_id,kind);


-- Rename domain objects in place; PostgreSQL preserves data and foreign-key references.
-- OAuth and Spring Session retain their framework-defined names.

ALTER TABLE users RENAME TO useraccount;
ALTER TABLE user_invitations RENAME TO userinvitation;
ALTER TABLE bootstrap_state RENAME TO bootstrapstate;
ALTER TABLE projects RENAME TO project;
ALTER TABLE profiles RENAME TO profile;
ALTER TABLE rules RENAME TO rule;
ALTER TABLE skills RENAME TO skill;
ALTER TABLE model_catalog RENAME TO model;
ALTER TABLE model_role_policies RENAME TO modelrolepolicie;
ALTER TABLE memories RENAME TO memory;
ALTER TABLE memory_proposals RENAME TO memoryproposal;
ALTER TABLE tasks RENAME TO task;
ALTER TABLE task_plans RENAME TO taskplan;
ALTER TABLE task_blocks RENAME TO taskblock;
ALTER TABLE checkpoints RENAME TO checkpoint;
ALTER TABLE review_findings RENAME TO reviewfinding;
ALTER TABLE local_clients RENAME TO localclient;
ALTER TABLE execution_approvals RENAME TO executionapproval;
ALTER TABLE model_consents RENAME TO modelconsent;
ALTER TABLE execution_events RENAME TO executionevent;
ALTER TABLE execution_evidence RENAME TO executionevidence;
ALTER TABLE checkpoint_sync RENAME TO checkpointsync;
ALTER TABLE administrative_audit RENAME TO administrativeaudit;

ALTER TABLE useraccount RENAME COLUMN password_hash TO passwordhash;
ALTER TABLE useraccount RENAME COLUMN created_at TO createdat;

ALTER TABLE userinvitation RENAME COLUMN token_hash TO tokenhash;
ALTER TABLE userinvitation RENAME COLUMN expires_at TO expiresat;
ALTER TABLE userinvitation RENAME COLUMN invited_by TO invitedbyid;
ALTER TABLE userinvitation RENAME COLUMN consumed_at TO consumedat;

ALTER TABLE bootstrapstate RENAME COLUMN created_at TO createdat;

ALTER TABLE executionapproval RENAME COLUMN owner_id TO ownerid;
ALTER TABLE executionapproval RENAME COLUMN task_id TO taskid;
ALTER TABLE executionapproval RENAME COLUMN plan_id TO planid;
ALTER TABLE executionapproval RENAME COLUMN plan_version TO planversion;
ALTER TABLE executionapproval RENAME COLUMN scope_hash TO scopehash;
ALTER TABLE executionapproval RENAME COLUMN rules_hash TO ruleshash;
ALTER TABLE executionapproval RENAME COLUMN created_at TO createdat;

ALTER TABLE modelconsent RENAME COLUMN owner_id TO ownerid;
ALTER TABLE modelconsent RENAME COLUMN task_id TO taskid;
ALTER TABLE modelconsent RENAME COLUMN block_id TO blockid;
ALTER TABLE modelconsent RENAME COLUMN delegation_id TO delegationid;
ALTER TABLE modelconsent RENAME COLUMN plan_id TO planid;
ALTER TABLE modelconsent RENAME COLUMN plan_version TO planversion;
ALTER TABLE modelconsent RENAME COLUMN model_id TO modelid;
ALTER TABLE modelconsent RENAME COLUMN expires_at TO expiresat;
ALTER TABLE modelconsent RENAME COLUMN consumed_at TO consumedat;

ALTER TABLE executionevent RENAME COLUMN owner_id TO ownerid;
ALTER TABLE executionevent RENAME COLUMN task_id TO taskid;
ALTER TABLE executionevent RENAME COLUMN client_id TO clientid;
ALTER TABLE executionevent RENAME COLUMN local_sequence TO localsequence;
ALTER TABLE executionevent RENAME COLUMN event_type TO eventtype;
ALTER TABLE executionevent RENAME COLUMN payload_hash TO payloadhash;
ALTER TABLE executionevent RENAME COLUMN created_at TO createdat;

ALTER TABLE executionevidence RENAME COLUMN owner_id TO ownerid;
ALTER TABLE executionevidence RENAME COLUMN task_id TO taskid;
ALTER TABLE executionevidence RENAME COLUMN block_id TO blockid;
ALTER TABLE executionevidence RENAME COLUMN event_id TO eventid;
ALTER TABLE executionevidence RENAME COLUMN created_at TO createdat;

ALTER TABLE checkpointsync RENAME COLUMN task_id TO taskid;
ALTER TABLE checkpointsync RENAME COLUMN client_id TO clientid;
ALTER TABLE checkpointsync RENAME COLUMN last_sequence TO lastsequence;
ALTER TABLE checkpointsync RENAME COLUMN remote_revision TO remoterevision;

ALTER TABLE administrativeaudit RENAME COLUMN owner_id TO ownerid;
ALTER TABLE administrativeaudit RENAME COLUMN resource_id TO resourceid;
ALTER TABLE administrativeaudit RENAME COLUMN created_at TO createdat;

ALTER TABLE project RENAME COLUMN owner_id TO ownerid;
ALTER TABLE project RENAME COLUMN project_id TO projectid;
ALTER TABLE project RENAME COLUMN updated_at TO updatedat;

ALTER TABLE profile RENAME COLUMN owner_id TO ownerid;
ALTER TABLE profile RENAME COLUMN project_id TO projectid;
ALTER TABLE profile RENAME COLUMN updated_at TO updatedat;

ALTER TABLE rule RENAME COLUMN owner_id TO ownerid;
ALTER TABLE rule RENAME COLUMN project_id TO projectid;
ALTER TABLE rule RENAME COLUMN updated_at TO updatedat;

ALTER TABLE skill RENAME COLUMN owner_id TO ownerid;
ALTER TABLE skill RENAME COLUMN project_id TO projectid;
ALTER TABLE skill RENAME COLUMN updated_at TO updatedat;

ALTER TABLE model RENAME COLUMN owner_id TO ownerid;
ALTER TABLE model RENAME COLUMN project_id TO projectid;
ALTER TABLE model RENAME COLUMN updated_at TO updatedat;

ALTER TABLE modelrolepolicie RENAME COLUMN owner_id TO ownerid;
ALTER TABLE modelrolepolicie RENAME COLUMN project_id TO projectid;
ALTER TABLE modelrolepolicie RENAME COLUMN updated_at TO updatedat;

ALTER TABLE memory RENAME COLUMN owner_id TO ownerid;
ALTER TABLE memory RENAME COLUMN project_id TO projectid;
ALTER TABLE memory RENAME COLUMN updated_at TO updatedat;

ALTER TABLE memoryproposal RENAME COLUMN owner_id TO ownerid;
ALTER TABLE memoryproposal RENAME COLUMN project_id TO projectid;
ALTER TABLE memoryproposal RENAME COLUMN updated_at TO updatedat;

ALTER TABLE task RENAME COLUMN owner_id TO ownerid;
ALTER TABLE task RENAME COLUMN project_id TO projectid;
ALTER TABLE task RENAME COLUMN updated_at TO updatedat;

ALTER TABLE taskplan RENAME COLUMN owner_id TO ownerid;
ALTER TABLE taskplan RENAME COLUMN project_id TO projectid;
ALTER TABLE taskplan RENAME COLUMN updated_at TO updatedat;

ALTER TABLE taskblock RENAME COLUMN owner_id TO ownerid;
ALTER TABLE taskblock RENAME COLUMN project_id TO projectid;
ALTER TABLE taskblock RENAME COLUMN updated_at TO updatedat;

ALTER TABLE checkpoint RENAME COLUMN owner_id TO ownerid;
ALTER TABLE checkpoint RENAME COLUMN project_id TO projectid;
ALTER TABLE checkpoint RENAME COLUMN updated_at TO updatedat;

ALTER TABLE reviewfinding RENAME COLUMN owner_id TO ownerid;
ALTER TABLE reviewfinding RENAME COLUMN project_id TO projectid;
ALTER TABLE reviewfinding RENAME COLUMN updated_at TO updatedat;

ALTER TABLE localclient RENAME COLUMN owner_id TO ownerid;
ALTER TABLE localclient RENAME COLUMN project_id TO projectid;
ALTER TABLE localclient RENAME COLUMN updated_at TO updatedat;

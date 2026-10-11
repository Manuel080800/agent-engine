CREATE TABLE modeldefault (
    modelid varchar(100) PRIMARY KEY,
    data jsonb NOT NULL,
    CHECK (data ->> 'modelId' = modelid)
);

INSERT INTO modeldefault(modelid, data) VALUES
    ('gpt-6-luna', '{"name":"GPT-6 Luna","modelId":"gpt-6-luna","efforts":["low","medium","high","xhigh"],"priority":1,"enabled":true,"requiresVerification":false}'),
    ('gpt-6.1-sol', '{"name":"GPT-6.1 Sol","modelId":"gpt-6.1-sol","efforts":["medium","high"],"priority":2,"enabled":true,"requiresVerification":true}');

-- Keep existing account customizations and tombstones intact.
INSERT INTO model(id, ownerid, data)
SELECT gen_random_uuid(), account.id, defaults.data
FROM useraccount account CROSS JOIN modeldefault defaults
WHERE NOT EXISTS (
    SELECT 1 FROM model existing
    WHERE existing.ownerid = account.id AND existing.projectid IS NULL
        AND existing.data ->> 'modelId' = defaults.modelid
);

CREATE FUNCTION initializemodeldefaults() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO model(id, ownerid, data)
    SELECT gen_random_uuid(), NEW.id, defaults.data FROM modeldefault defaults;
    RETURN NEW;
END;
$$;

CREATE TRIGGER initializemodeldefaults
AFTER INSERT ON useraccount FOR EACH ROW EXECUTE FUNCTION initializemodeldefaults();

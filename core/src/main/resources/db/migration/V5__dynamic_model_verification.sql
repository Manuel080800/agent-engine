-- Preserve each model's existing per-delegation consent requirement.
UPDATE model
SET data = (data - 'requiresConsent') || jsonb_build_object(
    'requiresVerification', COALESCE(data -> 'requiresVerification', data -> 'requiresConsent', 'false'::jsonb)
)
WHERE NOT data ? 'requiresVerification' OR data ? 'requiresConsent';

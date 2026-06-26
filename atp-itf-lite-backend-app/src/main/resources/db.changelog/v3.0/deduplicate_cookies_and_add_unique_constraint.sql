DELETE FROM cookies
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY key, domain, user_id, project_id
                   ORDER BY id DESC
               ) AS rn
        FROM cookies
        WHERE user_id IS NOT NULL AND project_id IS NOT NULL
    ) ranked
    WHERE rn > 1
);

ALTER TABLE cookies
    ADD CONSTRAINT cookies_key_domain_user_project_uq
    UNIQUE (key, domain, user_id, project_id);

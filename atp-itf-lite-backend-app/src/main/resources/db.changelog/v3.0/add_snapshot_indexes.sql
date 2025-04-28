CREATE INDEX IF NOT EXISTS jv_snapshot_global_id_fk_version_idx ON jv_snapshot(global_id_fk, version);

CREATE INDEX IF NOT EXISTS jv_snapshot_global_id_fk_version_idx_commit_fk ON jv_snapshot(global_id_fk, version, commit_fk);

CREATE INDEX IF NOT EXISTS jv_snapshot_type ON jv_snapshot(type);

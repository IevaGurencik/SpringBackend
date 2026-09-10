ALTER TABLE file_metadata DROP CONSTRAINT file_metadata_filename_key;
ALTER TABLE file_metadata ADD COLUMN stored_filename VARCHAR(255);
UPDATE file_metadata SET stored_filename = filename WHERE stored_filename IS NULL;
ALTER TABLE file_metadata ALTER COLUMN stored_filename SET NOT NULL;
ALTER TABLE file_metadata ADD CONSTRAINT file_metadata_stored_filename_key UNIQUE (stored_filename);
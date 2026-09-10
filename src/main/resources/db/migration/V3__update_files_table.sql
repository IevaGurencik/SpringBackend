ALTER TABLE file_metadata
ADD COLUMN todo_id BIGINT;

ALTER TABLE file_metadata
ADD CONSTRAINT fk_file_metadata_todo
    FOREIGN KEY (todo_id)
    REFERENCES to_do_entity(id)
    ON DELETE CASCADE;
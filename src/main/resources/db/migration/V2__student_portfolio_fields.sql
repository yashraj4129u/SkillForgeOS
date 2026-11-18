-- Additive migration for student portfolio links and resume uploads.
-- These nullable columns preserve every existing participant record.
ALTER TABLE participants
    ADD COLUMN github_url VARCHAR(255) NULL,
    ADD COLUMN resume_file_name VARCHAR(255) NULL,
    ADD COLUMN resume_file MEDIUMBLOB NULL;

-- Collaboration, milestone resources, and student submissions.
CREATE TABLE IF NOT EXISTS messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_user_id INT NOT NULL,
    recipient_user_id INT NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_messages_conversation (sender_user_id, recipient_user_id, sent_at)
);

ALTER TABLE milestones
    ADD COLUMN attachment_name VARCHAR(255) NULL,
    ADD COLUMN attachment MEDIUMBLOB NULL,
    ADD COLUMN resource_link VARCHAR(1000) NULL;

CREATE TABLE IF NOT EXISTS milestone_submissions (
    submission_id INT AUTO_INCREMENT PRIMARY KEY,
    milestone_id INT NOT NULL,
    participant_id VARCHAR(64) NOT NULL,
    submission_link VARCHAR(1000) NULL,
    file_name VARCHAR(255) NULL,
    file_data MEDIUMBLOB NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_milestone_participant (milestone_id, participant_id),
    CONSTRAINT fk_submission_milestone FOREIGN KEY (milestone_id)
        REFERENCES milestones(milestone_id) ON DELETE CASCADE,
    INDEX idx_submission_participant (participant_id)
);

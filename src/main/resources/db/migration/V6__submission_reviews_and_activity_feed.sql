ALTER TABLE milestone_submissions
    ADD COLUMN review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN review_feedback TEXT NULL;

CREATE TABLE IF NOT EXISTS activity_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    team_id INT NOT NULL,
    actor_user_id INT NULL,
    event_type VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_team_time (team_id, created_at),
    CONSTRAINT fk_activity_team FOREIGN KEY (team_id)
        REFERENCES teams(team_id) ON DELETE CASCADE
);

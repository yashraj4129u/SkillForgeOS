-- The legacy teams check required role quota fields to add up to four.
-- Membership is now flexible, so squad creation must not be blocked by it.
ALTER TABLE teams DROP CHECK teams_chk_5;

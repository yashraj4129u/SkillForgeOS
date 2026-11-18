package com.skillforge.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Writes timeline events on the same connection as the related action. */
final class ActivitySupport {
    private ActivitySupport() { }

    static void record(Connection connection, int teamId, Integer actorUserId,
                       String eventType, String description) {
        String sql = "INSERT INTO activity_events(team_id,actor_user_id,event_type,description) VALUES(?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            if (actorUserId == null) statement.setNull(2, java.sql.Types.INTEGER);
            else statement.setInt(2, actorUserId);
            statement.setString(3, eventType);
            String detail=description==null?"":description;
            statement.setString(4, detail.length()>500?detail.substring(0,500):detail);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // Timeline logging must never cancel the underlying squad or milestone action.
        }
    }
}

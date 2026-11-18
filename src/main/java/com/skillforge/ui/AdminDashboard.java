
package com.skillforge.ui;

import com.skillforge.db.DBConnection;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AdminDashboard extends JFrame {

    private final int userId;

    private final Color bg = AppTheme.BACKGROUND;
    private final Color panel = AppTheme.SURFACE;
    private final Color green = AppTheme.GREEN;
    private final Color blue = AppTheme.BLUE;
    private final Color red = AppTheme.RED;
    private final Color white = AppTheme.TEXT;

    private JLabel studentsCount;
    private JLabel mentorsCount;
    private JLabel squadsCount;
    private JLabel approvedCount;
    private JLabel fullCount;
    private JLabel readinessCount;

    private JTable studentTable;
    private JTable mentorTable;
    private JTable squadTable;

    private DefaultTableModel studentModel;
    private DefaultTableModel mentorModel;
    private DefaultTableModel squadModel;

    public AdminDashboard(int userId) {
        this.userId = userId;

        setTitle("SkillForge OS - Admin Dashboard");
        setSize(1320, 860);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(bg);
        root.setBorder(
                BorderFactory.createEmptyBorder(20, 22, 20, 22)
        );

        JButton logout = button("LOGOUT", red);
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        root.add(AppTheme.header("Admin Control Center",
                "Manage students, mentors, squads, and readiness", logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", overviewPanel());
        tabs.addTab("Students", studentsPanel());
        tabs.addTab("Mentors", mentorsPanel());
        tabs.addTab("Squads", squadsPanel());
        AppTheme.styleTabs(tabs);

        root.add(tabs, BorderLayout.CENTER);
        add(root);

        if (!verifyAdmin()) {
            JOptionPane.showMessageDialog(
                    this, "Admin access denied."
            );
            dispose();
            SwingUtilities.invokeLater(
                    () -> new LoginFrame().setVisible(true)
            );
            return;
        }

        refreshAll();
    }

    private boolean verifyAdmin() {
        String sql =
                "SELECT role FROM users WHERE user_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() &&
                        "ADMIN".equals(rs.getString("role"));
            }
        } catch (SQLException e) {
            error(e);
            return false;
        }
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        AppTheme.styleButton(b, color);
        return b;
    }

    private DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private JScrollPane scroll(JTable table) {
        return AppTheme.styleScrollPane(table);
    }

    private JPanel overviewPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBackground(bg);
        p.setBorder(
                BorderFactory.createEmptyBorder(26, 18, 24, 18)
        );

        JPanel cards = new JPanel(new GridLayout(2, 3, 18, 18));
        cards.setBackground(bg);

        studentsCount = new JLabel("0");
        mentorsCount = new JLabel("0");
        squadsCount = new JLabel("0");
        approvedCount = new JLabel("0");
        fullCount = new JLabel("0");
        readinessCount = new JLabel("0%");

        cards.add(statCard("TOTAL STUDENTS", studentsCount));
        cards.add(statCard("TOTAL MENTORS", mentorsCount));
        cards.add(statCard("TOTAL SQUADS", squadsCount));
        cards.add(statCard("APPROVED SQUADS", approvedCount));
        cards.add(statCard("SQUADS WITH 4+ MEMBERS", fullCount));
        cards.add(statCard("PIPELINE READINESS", readinessCount));

        JButton refresh = button("REFRESH ANALYTICS", blue);
        refresh.addActionListener(e -> refreshAll());

        p.add(cards, BorderLayout.CENTER);
        p.add(refresh, BorderLayout.SOUTH);

        return p;
    }

    private JPanel statCard(String label, JLabel value) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(panel);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.insets = new Insets(10, 10, 10, 10);

        JLabel heading = new JLabel(label);
        heading.setForeground(white);
        heading.setFont(
                new Font("SansSerif", Font.BOLD, 14)
        );

        value.setForeground(green);
        value.setFont(
                new Font("SansSerif", Font.BOLD, 36)
        );

        card.add(heading, g);
        g.gridy = 1;
        card.add(value, g);

        return card;
    }

    private JPanel studentsPanel() {
        studentModel = model(
                "Registration ID", "Name", "Username",
                "Primary Role", "Secondary Role",
                "Experience", "Squad", "Portfolio / Project Link", "Resume File"
        );

        studentTable = AppTheme.emptyTable(studentModel,"No student records yet.");
        AppTheme.styleTable(studentTable);
        AppTheme.setColumnWidths(studentTable,135,155,125,115,125,110,155,230,130);
        studentTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel p = tablePanel(studentTable);

        JButton export = button("EXPORT STUDENT CSV", green);
        export.addActionListener(e -> exportCSV(studentTable));

        JButton refresh = button("REFRESH", blue);
        refresh.addActionListener(e -> refreshAll());

        JPanel actions = new JPanel(
                new GridLayout(1, 2, 10, 10)
        );
        actions.setBackground(bg);
        JButton add = button("ADD STUDENT", green);
        JButton edit = button("EDIT STUDENT", blue);
        JButton delete = button("DELETE STUDENT", red);
        JButton downloadResume = button("DOWNLOAD RESUME", blue);

        add.addActionListener(e -> addStudent());
        edit.addActionListener(e -> editStudent());
        delete.addActionListener(e -> deleteStudent());
        downloadResume.addActionListener(e -> downloadStudentResume());
        edit.setEnabled(false); delete.setEnabled(false); downloadResume.setEnabled(false);
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                int count=studentTable.getSelectedRowCount();
                edit.setEnabled(count==1); delete.setEnabled(count>0); downloadResume.setEnabled(count==1);
            }
        });

        actions.setLayout(new GridLayout(1, 6, 10, 10));

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(downloadResume);
        actions.add(export);
        actions.add(refresh);

        p.add(actions, BorderLayout.SOUTH);
        return p;
    }



    private JPanel mentorsPanel() {
        mentorModel = model(
                "Mentor ID", "Name", "Username",
                "Specialization", "Max Squads",
                "Approved Squads", "Pending Squads"
        );

        mentorTable = AppTheme.emptyTable(mentorModel,"No mentor records yet.");
        AppTheme.styleTable(mentorTable);
        AppTheme.setColumnWidths(mentorTable,100,175,140,200,115,120,120);

        JPanel p = tablePanel(mentorTable);

        JPanel actions = new JPanel(
                new GridLayout(1, 4, 10, 10)
        );
        actions.setBackground(bg);
        actions.setBorder(
                BorderFactory.createEmptyBorder(10, 0, 0, 0)
        );

        JButton add = button("ADD MENTOR", green);
        JButton edit = button("EDIT MENTOR", blue);
        JButton delete = button("DELETE MENTOR", red);
        JButton refresh = button("REFRESH", blue);

        add.addActionListener(e -> addMentor());
        edit.addActionListener(e -> editMentor());
        delete.addActionListener(e -> deleteMentor());
        refresh.addActionListener(e -> refreshAll());
        edit.setEnabled(false); delete.setEnabled(false);
        mentorTable.getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) { edit.setEnabled(mentorTable.getSelectedRowCount()==1); delete.setEnabled(mentorTable.getSelectedRowCount()>0); }
        });

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refresh);

        p.add(actions, BorderLayout.SOUTH);

        return p;
    }



    private JPanel squadsPanel() {
        squadModel = model(
                "Squad ID", "Squad Name", "Track",
                "Mentor", "Members", "Approval Status"
        );

        squadTable = AppTheme.emptyTable(squadModel,"No squads have been created yet.");
        AppTheme.styleTable(squadTable);
        AppTheme.styleStatusColumn(squadTable, 5);
        AppTheme.setColumnWidths(squadTable,95,185,185,170,130,150);
        squadTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel p = tablePanel(squadTable);

        JPanel actions = new JPanel(new GridLayout(1, 6, 10, 10));
        actions.setBackground(bg);
        actions.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JButton add = button("ADD SQUAD", green);
        JButton edit = button("EDIT SQUAD", blue);
        JButton delete = button("DELETE SQUAD", red);
        JButton approve = button("APPROVE", green);
        JButton reject = button("REJECT", red);
        JButton refresh = button("REFRESH", blue);
        add.addActionListener(e -> addSquad());
        edit.addActionListener(e -> editSquad());
        delete.addActionListener(e -> deleteSquad());
        approve.addActionListener(e -> decideSquad(true));
        reject.addActionListener(e -> decideSquad(false));
        refresh.addActionListener(e -> refreshAll());
        edit.setEnabled(false); delete.setEnabled(false); approve.setEnabled(false); reject.setEnabled(false);
        squadTable.getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                int count=squadTable.getSelectedRowCount(); edit.setEnabled(count==1);
                delete.setEnabled(count>0); approve.setEnabled(count>0); reject.setEnabled(count>0);
            }
        });
        actions.add(add); actions.add(edit); actions.add(delete);
        actions.add(approve); actions.add(reject); actions.add(refresh);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private Integer selectedSquadId() {
        int[] selected = squadTable.getSelectedRows();
        if (selected.length != 1) {
            JOptionPane.showMessageDialog(this, "Select exactly one squad to edit.");
            return null;
        }
        int view = selected[0];
        return Integer.valueOf(squadModel.getValueAt(
                squadTable.convertRowIndexToModel(view), 0).toString());
    }

    private List<Object> selectedValues(JTable table, DefaultTableModel model, int column) {
        List<Object> values = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            values.add(model.getValueAt(table.convertRowIndexToModel(viewRow), column));
        }
        if (values.isEmpty()) JOptionPane.showMessageDialog(this, "Select one or more rows first.");
        return values;
    }

    private void addSquad() {
        JTextField name = new JTextField();
        JTextField track = new JTextField();
        JSpinner frontend = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JSpinner backend = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JSpinner ai = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JSpinner devops = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JComboBox<MentorChoice> mentor = mentorChoices();
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Squad name:")); form.add(name);
        form.add(new JLabel("Track:")); form.add(track);
        form.add(new JLabel("Suggested Frontend mix:")); form.add(frontend);
        form.add(new JLabel("Suggested Backend mix:")); form.add(backend);
        form.add(new JLabel("Suggested AI mix:")); form.add(ai);
        form.add(new JLabel("Suggested DevOps mix:")); form.add(devops);
        form.add(new JLabel("Mentor:")); form.add(mentor);
        JLabel mixHint=new JLabel("Squad membership is unlimited; these values are a skill-mix guide.");
        mixHint.setForeground(AppTheme.MUTED); form.add(mixHint); form.add(new JLabel(""));
        if (AppTheme.showFormDialog(this, form, "Add Squad") != JOptionPane.OK_OPTION) return;
        String value = name.getText().trim();
        MentorChoice choice = (MentorChoice) mentor.getSelectedItem();
        if (value.isEmpty() || value.length() > 100 || track.getText().isBlank() || choice == null) {
            JOptionPane.showMessageDialog(this, "Enter the squad name and track, and select a mentor."); return;
        }
        int f = (int) frontend.getValue(), b = (int) backend.getValue();
        int a = (int) ai.getValue(), d = (int) devops.getValue();
        if (f + b + a + d != 4) {
            JOptionPane.showMessageDialog(this, "Set the four suggested role openings to a total of 4, then try again."); return;
        }
        String sql = "INSERT INTO teams (team_name,track,mentor_id,max_frontend,max_backend,max_ai,max_devops,approval_status) VALUES (?,?,?,?,?,?,?,'PENDING')";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement lock = c.prepareStatement("SELECT max_squads FROM mentors WHERE mentor_id=? FOR UPDATE")) {
                lock.setInt(1, choice.id);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Mentor no longer exists.");
                    int cap = rs.getInt(1);
                    try (PreparedStatement count = c.prepareStatement("SELECT COUNT(*) FROM teams WHERE mentor_id=? AND approval_status IN ('PENDING','APPROVED')")) {
                        count.setInt(1, choice.id);
                        try (ResultSet used = count.executeQuery()) {
                            used.next(); if (used.getInt(1) >= cap) throw new SQLException("Selected mentor has reached squad capacity.");
                        }
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value); ps.setString(2, track.getText().trim());
            ps.setInt(3, choice.id);
            ps.setInt(4, f); ps.setInt(5, b); ps.setInt(6, a); ps.setInt(7, d); ps.executeUpdate();
            }
            c.commit(); refreshAll();
        } catch (SQLException e) { error(e); }
    }

    private void editSquad() {
        Integer id = selectedSquadId(); if (id == null) return;
        int row = squadTable.convertRowIndexToModel(squadTable.getSelectedRow());
        JTextField name = new JTextField(String.valueOf(squadModel.getValueAt(row, 1)));
        JTextField track = new JTextField(String.valueOf(squadModel.getValueAt(row, 2)));
        JComboBox<MentorChoice> mentor = mentorChoices();
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Squad name:")); form.add(name); form.add(new JLabel("Track:")); form.add(track); form.add(new JLabel("Mentor:")); form.add(mentor);
        if (AppTheme.showFormDialog(this, form, "Edit Squad") != JOptionPane.OK_OPTION) return;
        MentorChoice choice = (MentorChoice) mentor.getSelectedItem(); String value = name.getText().trim();
        if (value.isEmpty() || value.length() > 100 || track.getText().isBlank() || choice == null) { JOptionPane.showMessageDialog(this, "Enter the squad name and track, and select a mentor."); return; }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("UPDATE teams SET team_name=?,track=?,mentor_id=? WHERE team_id=?")) {
                ps.setString(1, value); ps.setString(2, track.getText().trim()); ps.setInt(3, choice.id); ps.setInt(4, id);
                if (ps.executeUpdate() != 1) throw new SQLException("Squad no longer exists.");
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            refreshAll();
        } catch (SQLException e) { error(e); }
    }

    private void deleteSquad() {
        List<Object> ids = selectedValues(squadTable, squadModel, 0);
        if (ids.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Delete " + ids.size() + " selected squad(s)? Their members will be unassigned.", "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Object value : ids) {
                    int id = Integer.parseInt(value.toString());
                    try (PreparedStatement lock = c.prepareStatement("SELECT team_id FROM teams WHERE team_id=? FOR UPDATE")) {
                        lock.setInt(1, id);
                        try (ResultSet rs = lock.executeQuery()) { if (!rs.next()) throw new SQLException("A selected squad no longer exists. Nothing was deleted."); }
                    }
                    try (PreparedStatement ps = c.prepareStatement("UPDATE participants SET team_id=NULL WHERE team_id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM teams WHERE team_id=?")) {
                        ps.setInt(1, id);
                        if (ps.executeUpdate() != 1) throw new SQLException("A selected squad could not be deleted.");
                    }
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            refreshAll();
        } catch (SQLException e) { error(e); }
    }

    private void decideSquad(boolean approve) {
        List<Object> ids = selectedValues(squadTable, squadModel, 0);
        if (ids.isEmpty()) return;
        String status = approve ? "APPROVED" : "REJECTED";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Object value : ids) {
                    int id = Integer.parseInt(value.toString());
                    int mentorId;
                    try (PreparedStatement ps = c.prepareStatement("SELECT mentor_id FROM teams WHERE team_id=? AND approval_status='PENDING' FOR UPDATE")) {
                        ps.setInt(1, id);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Every selected squad must be pending. No changes were saved.");
                            mentorId = rs.getInt(1);
                        }
                    }
                    if (approve) {
                        int cap;
                        try (PreparedStatement ps = c.prepareStatement("SELECT max_squads FROM mentors WHERE mentor_id=? FOR UPDATE")) {
                            ps.setInt(1, mentorId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) throw new SQLException("Assigned mentor no longer exists.");
                                cap = rs.getInt(1);
                            }
                        }
                        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM teams WHERE mentor_id=? AND approval_status='APPROVED'")) {
                            ps.setInt(1, mentorId);
                            try (ResultSet rs = ps.executeQuery()) {
                                rs.next();
                                if (rs.getInt(1) >= cap) throw new SQLException("A mentor has reached the approved squad limit. No changes were saved.");
                            }
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement("UPDATE teams SET approval_status=? WHERE team_id=? AND approval_status='PENDING'")) {
                        ps.setString(1, status); ps.setInt(2, id);
                        if (ps.executeUpdate() != 1) throw new SQLException("Squad status changed. No changes were saved.");
                    }
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            refreshAll();
        } catch (SQLException e) { error(e); }
    }

    private JComboBox<MentorChoice> mentorChoices() {
        JComboBox<MentorChoice> box = new JComboBox<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT m.mentor_id,u.full_name FROM mentors m JOIN users u ON m.user_id=u.user_id ORDER BY u.full_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) box.addItem(new MentorChoice(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) { error(e); }
        return box;
    }

    private static class MentorChoice {
        final int id; final String name;
        MentorChoice(int id, String name) { this.id = id; this.name = name; }
        public String toString() { return name; }
    }

    private JPanel tablePanel(JTable table) {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(bg);
        p.setBorder(
                BorderFactory.createEmptyBorder(15, 10, 15, 10)
        );

        JTextField search = new JTextField();
        search.setBackground(AppTheme.SURFACE);
        search.setForeground(white);
        search.setCaretColor(green);
        search.setFont(
                new Font("SansSerif", Font.PLAIN, 15)
        );
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        if (table == studentTable || table == squadTable) {
            table.setToolTipText("Select multiple rows with Ctrl-click or Shift-click.");
        }

        JPanel searchPanel = new JPanel(
                new BorderLayout(10, 10)
        );
        searchPanel.setBackground(bg);

        JLabel label = new JLabel("LIVE SEARCH:");
        label.setForeground(white);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));

        searchPanel.add(label, BorderLayout.WEST);
        searchPanel.add(search, BorderLayout.CENTER);

        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(
                        (DefaultTableModel) table.getModel()
                );

        table.setRowSorter(sorter);

        search.getDocument().addDocumentListener(
                new DocumentListener() {

                    private void filter() {
                        String text = search.getText().trim();

                        if (text.isEmpty()) {
                            sorter.setRowFilter(null);
                        } else {
                            sorter.setRowFilter(
                                    RowFilter.regexFilter(
                                            "(?i)" + Pattern.quote(text)
                                    )
                            );
                        }
                    }

                    public void insertUpdate(DocumentEvent e) {
                        filter();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        filter();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        filter();
                    }
                }
        );

        p.add(searchPanel, BorderLayout.NORTH);
        p.add(scroll(table), BorderLayout.CENTER);

        return p;
    }

    private void refreshAll() {
        loadOverview();
        loadStudents();
        loadMentors();
        loadSquads();
    }

    private void loadOverview() {
        String sql = """
            SELECT
            (SELECT COUNT(*) FROM participants),
            (SELECT COUNT(*) FROM mentors),
            (SELECT COUNT(*) FROM teams),
            (SELECT COUNT(*) FROM teams
             WHERE approval_status='APPROVED'),
            (SELECT COUNT(*) FROM (
                SELECT team_id
                FROM participants
                WHERE team_id IS NOT NULL
                GROUP BY team_id
            HAVING COUNT(*)>=4
            ) full_teams),
            (SELECT COUNT(*) FROM teams t
             WHERE t.approval_status='APPROVED'
             AND (SELECT COUNT(*) FROM participants p
                  WHERE p.team_id=t.team_id)>=4)
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int total = rs.getInt(3);
                int ready = rs.getInt(6);

                double percentage =
                        total == 0 ? 0 :
                                (ready * 100.0 / total);

                studentsCount.setText("" + rs.getInt(1));
                mentorsCount.setText("" + rs.getInt(2));
                squadsCount.setText("" + total);
                approvedCount.setText("" + rs.getInt(4));
                fullCount.setText("" + rs.getInt(5));

                readinessCount.setText(
                        String.format("%.1f%%", percentage)
                );
            }
        } catch (SQLException e) {
            error(e);
        }
    }

    private void loadStudents() {
        studentModel.setRowCount(0);

        String sql = """
            SELECT p.participant_id, p.name,
                   u.username, p.primary_role,
                   p.secondary_role, p.experience_level,
                   t.team_name, p.github_url, p.resume_file_name
            FROM participants p
            LEFT JOIN users u ON p.user_id=u.user_id
            LEFT JOIN teams t ON p.team_id=t.team_id
            ORDER BY p.name
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                studentModel.addRow(new Object[]{
                        rs.getString("participant_id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("primary_role"),
                        rs.getString("secondary_role"),
                        rs.getString("experience_level"),
                        rs.getString("team_name"),
                        rs.getString("github_url"),
                        rs.getString("resume_file_name")
                });
            }
        } catch (SQLException e) {
            error(e);
        }
    }

    private void loadMentors() {
        mentorModel.setRowCount(0);

        String sql = """
            SELECT m.mentor_id, u.full_name,
                   u.username, m.specialization,
                   m.max_squads,
                   (SELECT COUNT(*) FROM teams t
                    WHERE t.mentor_id=m.mentor_id
                    AND t.approval_status='APPROVED')
                    AS approved_count,
                   (SELECT COUNT(*) FROM teams t
                    WHERE t.mentor_id=m.mentor_id
                    AND t.approval_status='PENDING')
                    AS pending_count
            FROM mentors m
            JOIN users u ON m.user_id=u.user_id
            ORDER BY u.full_name
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                mentorModel.addRow(new Object[]{
                        rs.getInt("mentor_id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("specialization"),
                        rs.getInt("max_squads"),
                        rs.getInt("approved_count"),
                        rs.getInt("pending_count")
                });
            }
        } catch (SQLException e) {
            error(e);
        }
    }

    private void loadSquads() {
        squadModel.setRowCount(0);

        String sql = """
            SELECT t.team_id, t.team_name,
                   t.track, u.full_name,
                   t.approval_status, t.max_frontend, t.max_backend,
                   t.max_ai, t.max_devops,
                   (SELECT COUNT(*) FROM participants p
                    WHERE p.team_id=t.team_id)
                    AS member_count
            FROM teams t
            LEFT JOIN mentors m ON t.mentor_id=m.mentor_id
            LEFT JOIN users u ON m.user_id=u.user_id
            ORDER BY t.team_id
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                squadModel.addRow(new Object[]{
                        rs.getInt("team_id"),
                        rs.getString("team_name"),
                        rs.getString("track"),
                        rs.getString("full_name"),
                        rs.getInt("member_count") + " members",
                        rs.getString("approval_status")
                });
            }
        } catch (SQLException e) {
            error(e);
        }
    }

    private void exportCSV(JTable table) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(
                new File("SkillForge_Student_Roster.csv")
        );

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this, "Overwrite existing file?",
                    "Confirm Export",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file),
                        StandardCharsets.UTF_8
                ))) {

            for (int col = 0; col < table.getColumnCount(); col++) {
                if (col > 0) writer.print(",");
                writer.print(csv(table.getColumnName(col)));
            }
            writer.println();

            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    if (col > 0) writer.print(",");

                    Object value = table.getValueAt(row, col);
                    writer.print(csv(
                            value == null ? "" : value.toString()
                    ));
                }
                writer.println();
            }

            if (writer.checkError())
                throw new IOException("CSV write failed.");

            JOptionPane.showMessageDialog(
                    this, "CSV exported successfully!"
            );

        } catch (IOException e) {
            error(e);
        }
    }

    private String csv(String value) {
        String clean = value.replace("\r", " ")
                .replace("\n", " ");

        // Prevent spreadsheet formula injection.
        if (!clean.isEmpty() &&
                "=+-@\t".indexOf(clean.charAt(0)) >= 0) {
            clean = "'" + clean;
        }

        return "\"" + clean.replace("\"", "\"\"") + "\"";
    }

    private String selectedStudentId() {
        int[] selected = studentTable.getSelectedRows();
        if (selected.length != 1) {
            JOptionPane.showMessageDialog(
                    this, "Select exactly one student to edit.");
            return null;
        }
        int row = selected[0];

        int modelRow = studentTable.convertRowIndexToModel(row);
        return studentModel.getValueAt(modelRow, 0).toString();
    }

    private void addStudent() {
        JTextField reg = new JTextField();
        JTextField name = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        JComboBox<String> primary = new JComboBox<>(
                new String[]{"Frontend", "Backend", "AI", "DevOps"});
        JComboBox<String> secondary = new JComboBox<>(
                new String[]{"Frontend", "Backend", "AI", "DevOps"});
        JComboBox<String> level = new JComboBox<>(
                new String[]{"Beginner", "Intermediate", "Advanced"});
        JTextField github = new JTextField();
        JLabel resumeName = new JLabel("No resume selected");
        File[] resume = new File[1];
        JButton browseResume = button("CHOOSE FILE", blue);
        browseResume.addActionListener(e -> {
            File chosen = PortfolioSupport.chooseResume(this, resumeName);
            if (chosen != null) resume[0] = chosen;
        });
        JPanel resumeRow = new JPanel(new BorderLayout(8, 0));
        resumeRow.setBackground(AppTheme.SURFACE);
        resumeRow.add(browseResume, BorderLayout.WEST);
        resumeRow.add(resumeName, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Registration ID:"));
        form.add(reg);
        form.add(new JLabel("Full Name:"));
        form.add(name);
        form.add(new JLabel("Username:"));
        form.add(username);
        form.add(new JLabel("Password:"));
        form.add(password);
        form.add(new JLabel("Primary Role:"));
        form.add(primary);
        form.add(new JLabel("Secondary Role:"));
        form.add(secondary);
        form.add(new JLabel("Experience:"));
        form.add(level);
        form.add(new JLabel("Portfolio / Project Link:"));
        form.add(github);
        form.add(new JLabel("Resume (optional):"));
        form.add(resumeRow);

        int result = AppTheme.showFormDialog(this, form, "Add Student");

        if (result != JOptionPane.OK_OPTION) return;

        String regId = reg.getText().trim();
        String fullName = name.getText().trim();
        String login = username.getText().trim();
        char[] passwordChars = password.getPassword();

        if (regId.isEmpty() || regId.length() > 20 ||
                fullName.isEmpty() || fullName.length() > 100 ||
                login.isEmpty() || passwordChars.length == 0 ||
                primary.getSelectedItem().equals(secondary.getSelectedItem())) {
            java.util.Arrays.fill(passwordChars, '\0');
            JOptionPane.showMessageDialog(this,
                    "Check all fields. Primary and secondary roles must differ.");
            return;
        }

        // Compatible with the existing plain-text login.
        // Password hashing should be introduced across registration
        // and login together before production deployment.
        String pass = new String(passwordChars);
        java.util.Arrays.fill(passwordChars, '\0');

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                int newUserId;

                String userSql = """
                    INSERT INTO users
                    (username, password, role, full_name)
                    VALUES (?, ?, 'STUDENT', ?)
                    """;

                try (PreparedStatement ps = c.prepareStatement(
                        userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, login);
                    ps.setString(2, pass);
                    ps.setString(3, fullName);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next())
                            throw new SQLException("User ID not generated.");
                        newUserId = keys.getInt(1);
                    }
                }

                String studentSql = """
                    INSERT INTO participants
                    (participant_id, user_id, name,
                     primary_role, secondary_role, experience_level,
                     team_id, github_url, resume_file_name, resume_file)
                    VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
                    """;

                try (PreparedStatement ps = c.prepareStatement(studentSql)) {
                    ps.setString(1, regId);
                    ps.setInt(2, newUserId);
                    ps.setString(3, fullName);
                    ps.setString(4, primary.getSelectedItem().toString());
                    ps.setString(5, secondary.getSelectedItem().toString());
                    ps.setString(6, level.getSelectedItem().toString());
                    ps.setString(7, github.getText().trim().isEmpty() ? null : github.getText().trim());
                    PortfolioSupport.bindResume(ps, 8, 9, resume[0]);
                    ps.executeUpdate();
                }

                c.commit();
                JOptionPane.showMessageDialog(this, "Student added!");
                refreshAll();

            } catch (SQLException ex) {
                c.rollback();
                error(ex);
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void editStudent() {
        String regId = selectedStudentId();
        if (regId == null) return;

        int row = studentTable.convertRowIndexToModel(
                studentTable.getSelectedRow());

        JTextField name = new JTextField(
                String.valueOf(studentModel.getValueAt(row, 1)));

        JComboBox<String> primary = new JComboBox<>(
                new String[]{"Frontend", "Backend", "AI", "DevOps"});
        JComboBox<String> secondary = new JComboBox<>(
                new String[]{"Frontend", "Backend", "AI", "DevOps"});
        JComboBox<String> level = new JComboBox<>(
                new String[]{"Beginner", "Intermediate", "Advanced"});

        primary.setSelectedItem(studentModel.getValueAt(row, 3));
        secondary.setSelectedItem(studentModel.getValueAt(row, 4));
        level.setSelectedItem(studentModel.getValueAt(row, 5));
        Object githubValue = studentModel.getValueAt(row, 7);
        JTextField github = new JTextField(githubValue == null ? "" : githubValue.toString());
        JLabel resumeName = new JLabel("Keep existing resume");
        File[] resume = new File[1];
        JButton browseResume = button("REPLACE FILE", blue);
        browseResume.addActionListener(e -> {
            File chosen = PortfolioSupport.chooseResume(this, resumeName);
            if (chosen != null) resume[0] = chosen;
        });
        JPanel resumeRow = new JPanel(new BorderLayout(8, 0));
        resumeRow.setBackground(AppTheme.SURFACE);
        resumeRow.add(browseResume, BorderLayout.WEST);
        resumeRow.add(resumeName, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Full Name:"));
        form.add(name);
        form.add(new JLabel("Primary Role:"));
        form.add(primary);
        form.add(new JLabel("Secondary Role:"));
        form.add(secondary);
        form.add(new JLabel("Experience:"));
        form.add(level);
        form.add(new JLabel("Portfolio / Project Link:"));
        form.add(github);
        form.add(new JLabel("Replace resume (optional):"));
        form.add(resumeRow);

        if (AppTheme.showFormDialog(this, form, "Edit Student") != JOptionPane.OK_OPTION) {
            return;
        }

        String fullName = name.getText().trim();

        if (fullName.isEmpty() || fullName.length() > 100 ||
                primary.getSelectedItem().equals(secondary.getSelectedItem())) {
            JOptionPane.showMessageDialog(this, "Invalid student details.");
            return;
        }

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                // Editing a team member's skills could violate squad quotas.
                // Require removal from the squad before changing these fields.
                int linkedUserId;
                Integer teamId;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user_id, team_id FROM participants " +
                                "WHERE participant_id=? FOR UPDATE")) {
                    ps.setString(1, regId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Student no longer exists.");

                        linkedUserId = rs.getInt("user_id");
                        int value = rs.getInt("team_id");
                        teamId = rs.wasNull() ? null : value;
                    }
                }

                if (teamId != null) {
                    c.rollback();
                    JOptionPane.showMessageDialog(this,
                            "Remove this student from their squad before " +
                                    "editing their role or experience.");
                    return;
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE participants SET name=?, primary_role=?, " +
                                "secondary_role=?, experience_level=?, github_url=?, " +
                                "resume_file_name=COALESCE(?, resume_file_name), " +
                                "resume_file=COALESCE(?, resume_file) " +
                                "WHERE participant_id=?")) {
                    ps.setString(1, fullName);
                    ps.setString(2, primary.getSelectedItem().toString());
                    ps.setString(3, secondary.getSelectedItem().toString());
                    ps.setString(4, level.getSelectedItem().toString());
                    ps.setString(5, github.getText().trim().isEmpty() ? null : github.getText().trim());
                    PortfolioSupport.bindResume(ps, 6, 7, resume[0]);
                    ps.setString(8, regId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET full_name=? WHERE user_id=?")) {
                    ps.setString(1, fullName);
                    ps.setInt(2, linkedUserId);
                    ps.executeUpdate();
                }

                c.commit();
                JOptionPane.showMessageDialog(this, "Student updated!");
                refreshAll();

            } catch (SQLException ex) {
                c.rollback();
                error(ex);
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void deleteStudent() {
        List<Object> regIds = selectedValues(studentTable, studentModel, 0);
        if (regIds.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Permanently delete " + regIds.size() + " selected student(s) and their login accounts?",
                "Confirm Student Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                for (Object selectedId : regIds) {
                    String regId = selectedId.toString();
                    int linkedUserId;
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT user_id FROM participants WHERE participant_id=? FOR UPDATE")) {
                        ps.setString(1, regId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("A selected student no longer exists. Nothing was deleted.");
                            linkedUserId = rs.getInt(1);
                        }
                    }
                    // ON DELETE CASCADE removes the participant profile.
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM users WHERE user_id=? AND role='STUDENT'")) {
                        ps.setInt(1, linkedUserId);
                        if (ps.executeUpdate() != 1) throw new SQLException("A selected student account could not be deleted.");
                    }
                }

                c.commit();
                JOptionPane.showMessageDialog(this, regIds.size() + " student(s) deleted.");
                refreshAll();

            } catch (SQLException ex) {
                c.rollback();
                error(ex);
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void downloadStudentResume() {
        String regId = selectedStudentId();
        if (regId == null) return;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT resume_file_name, resume_file FROM participants WHERE participant_id=?")) {
            ps.setString(1, regId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getBytes("resume_file") == null) {
                    JOptionPane.showMessageDialog(this, "This student has not uploaded a resume.");
                    return;
                }
                String fileName = rs.getString("resume_file_name");
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save student resume");
                chooser.setSelectedFile(new File(fileName == null ? "resume" : fileName));
                if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
                java.nio.file.Files.write(chooser.getSelectedFile().toPath(), rs.getBytes("resume_file"));
                JOptionPane.showMessageDialog(this, "Resume saved successfully.");
            }
        } catch (SQLException | IOException e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "SkillForge Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void addMentor() {
        JTextField name = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField specialization = new JTextField();
        JTextField maxSquads = new JTextField("3");

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));

        form.add(new JLabel("Full Name:"));
        form.add(name);
        form.add(new JLabel("Username:"));
        form.add(username);
        form.add(new JLabel("Password:"));
        form.add(password);
        form.add(new JLabel("Specialization:"));
        form.add(specialization);
        form.add(new JLabel("Maximum Squads:"));
        form.add(maxSquads);

        int result = AppTheme.showFormDialog(this, form, "ADD MENTOR");

        if (result != JOptionPane.OK_OPTION) return;

        String fullName = name.getText().trim();
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String spec = specialization.getText().trim();

        if (fullName.isEmpty() || user.isEmpty()
                || pass.isEmpty() || spec.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.");
            return;
        }

        try {
            int max = Integer.parseInt(maxSquads.getText().trim());

            if (max < 1) throw new NumberFormatException();

            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);

                try {
                    int userId;

                    String sql = """
                        INSERT INTO users
                        (username, password, role, full_name)
                        VALUES (?, ?, 'MENTOR', ?)
                        """;

                    try (PreparedStatement ps =
                                 c.prepareStatement(sql,
                                         java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, user);
                        ps.setString(2, pass);
                        ps.setString(3, fullName);
                        ps.executeUpdate();

                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (!rs.next()) {
                                throw new SQLException("User ID not generated.");
                            }
                            userId = rs.getInt(1);
                        }
                    }

                    String mentorSql = """
                        INSERT INTO mentors
                        (user_id, specialization, max_squads)
                        VALUES (?, ?, ?)
                        """;

                    try (PreparedStatement ps = c.prepareStatement(mentorSql)) {
                        ps.setInt(1, userId);
                        ps.setString(2, spec);
                        ps.setInt(3, max);
                        ps.executeUpdate();
                    }

                    c.commit();
                    JOptionPane.showMessageDialog(this,
                            "Mentor added successfully!");

                } catch (Exception ex) {
                    c.rollback();
                    throw ex;
                }
            }

            refreshAll();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Maximum squads must be a valid positive number.");
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void editMentor() {
        int row = mentorTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select a mentor first.");
            return;
        }

        row = mentorTable.convertRowIndexToModel(row);

        int mentorId = Integer.parseInt(
                mentorModel.getValueAt(row, 0).toString()
        );

        JTextField name = new JTextField(
                mentorModel.getValueAt(row, 1).toString()
        );

        JTextField specialization = new JTextField(
                mentorModel.getValueAt(row, 3).toString()
        );

        JTextField maxSquads = new JTextField(
                mentorModel.getValueAt(row, 4).toString()
        );

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));

        form.add(new JLabel("Full Name:"));
        form.add(name);
        form.add(new JLabel("Specialization:"));
        form.add(specialization);
        form.add(new JLabel("Maximum Squads:"));
        form.add(maxSquads);

        int result = AppTheme.showFormDialog(this, form, "EDIT MENTOR");

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String fullName = name.getText().trim();
            String spec = specialization.getText().trim();
            int max = Integer.parseInt(maxSquads.getText().trim());

            if (fullName.isEmpty() || spec.isEmpty() || max < 1) {
                JOptionPane.showMessageDialog(this,
                        "Enter valid details.");
                return;
            }

            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);

                try {
                    String sql = """
                        UPDATE users u
                        JOIN mentors m ON u.user_id = m.user_id
                        SET u.full_name = ?,
                            m.specialization = ?,
                            m.max_squads = ?
                        WHERE m.mentor_id = ?
                        """;

                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setString(1, fullName);
                        ps.setString(2, spec);
                        ps.setInt(3, max);
                        ps.setInt(4, mentorId);
                        ps.executeUpdate();
                    }

                    c.commit();
                } catch (Exception ex) {
                    c.rollback();
                    throw ex;
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Mentor updated successfully!");

            refreshAll();

        } catch (Exception ex) {
            error(ex);
        }
    }

    private void deleteMentor() {
        int row = mentorTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select a mentor first.");
            return;
        }

        row = mentorTable.convertRowIndexToModel(row);

        int mentorId = Integer.parseInt(
                mentorModel.getValueAt(row, 0).toString()
        );

        String mentorName = mentorModel.getValueAt(row, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete mentor " + mentorName + "?\n"
                        + "This action cannot be undone.",
                "CONFIRM DELETE",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                String check = """
                    SELECT COUNT(*)
                    FROM teams
                    WHERE mentor_id = ?
                    """;

                try (PreparedStatement ps = c.prepareStatement(check)) {
                    ps.setInt(1, mentorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();

                        if (rs.getInt(1) > 0) {
                            throw new SQLException(
                                    "Cannot delete this mentor because teams "
                                            + "are assigned to them. Reassign "
                                            + "the teams first."
                            );
                        }
                    }
                }

                int userId;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user_id FROM mentors WHERE mentor_id = ?")) {
                    ps.setInt(1, mentorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Mentor not found.");
                        }
                        userId = rs.getInt("user_id");
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM mentors WHERE mentor_id = ?")) {
                    ps.setInt(1, mentorId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM users WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                c.commit();

                JOptionPane.showMessageDialog(this,
                        "Mentor deleted successfully!");

            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }

            refreshAll();

        } catch (Exception ex) {
            error(ex);
        }
    }

}

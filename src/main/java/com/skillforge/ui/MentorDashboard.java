
package com.skillforge.ui;

import com.skillforge.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MentorDashboard extends JFrame {

    private final int userId;
    private int mentorId;
    private int maxSquads;

    private static final Color BG = AppTheme.BACKGROUND;
    private static final Color PANEL = AppTheme.SURFACE;
    private static final Color GREEN = AppTheme.GREEN;
    private static final Color BLUE = AppTheme.BLUE;
    private static final Color RED = AppTheme.RED;
    private static final Color AMBER = AppTheme.AMBER;
    private static final Color WHITE = AppTheme.TEXT;

    private JLabel hud;
    private JTable pendingTable, activeTable, memberTable, milestoneTable;
    private DefaultTableModel pendingModel, activeModel;
    private DefaultTableModel memberModel, milestoneModel;
    private DefaultTableModel activityModel;
    private JProgressBar selectedProgress;
    private JLabel selectedTeamSummary, selectedOverdue;
    private JButton inspectPendingAction, acceptPendingAction, rejectPendingAction;
    private JButton addMemberAction, removeMemberAction, editMemberAction;
    private JButton updateMilestoneAction, viewSubmissionAction, reviewWorkAction;

    private final List<Integer> pendingIds = new ArrayList<>();
    private final List<Integer> activeIds = new ArrayList<>();
    private final List<String> memberIds = new ArrayList<>();
    private final List<Integer> milestoneIds = new ArrayList<>();
    private final List<Integer> milestoneTeamIds = new ArrayList<>();

    private int selectedActiveTeam = -1;

    public MentorDashboard(int userId) {
        this.userId = userId;

        setTitle("SkillForge OS - Mentor Dashboard");
        setSize(1280, 840);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JButton logout = button("LOGOUT", RED);
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        hud = new JLabel("Loading mentor information...");
        hud.setForeground(WHITE);
        hud.setFont(new Font("SansSerif", Font.BOLD, 16));
        hud.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        hud.setOpaque(true);
        hud.setBackground(PANEL);

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(BG);
        north.add(AppTheme.header("Mentor Portal",
                "Create squads, guide teams, and track milestones", logout), BorderLayout.NORTH);
        north.add(hud, BorderLayout.SOUTH);

        root.add(north, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pending Applications", pendingPanel());
        tabs.addTab("Active Squads", activePanel());
        tabs.addTab("Milestones & Feedback", milestonePanel());
        tabs.addTab("Squad Activity", activityPanel());
        AppTheme.styleTabs(tabs);

        root.add(tabs, BorderLayout.CENTER);
        add(root);

        loadMentor();
        refreshAll();
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        AppTheme.styleButton(b, color);
        return b;
    }

    private DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JScrollPane scroll(JTable table) {
        return AppTheme.styleScrollPane(table);
    }

    private JPanel actions(JButton... buttons) {
        JPanel p = new JPanel(new GridLayout(Math.max(1, (buttons.length + 2) / 3), 3, 10, 10));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        for (JButton b : buttons) p.add(b);
        return p;
    }

    private JComponent searchStrip(JTable table) {
        JTextField search = new JTextField();
        search.setToolTipText("Search rows");
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER,1,true),
                BorderFactory.createEmptyBorder(8,11,8,11)));
        search.setBackground(PANEL); search.setForeground(WHITE); search.setCaretColor(GREEN);
        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String value=search.getText().trim();
                sorter.setRowFilter(value.isEmpty()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(value)));
            }
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        JPanel strip = new JPanel(new BorderLayout(8,0)); strip.setOpaque(false);
        JLabel label=new JLabel("SEARCH"); label.setForeground(AppTheme.MUTED);
        label.setFont(new Font("SansSerif",Font.BOLD,10));
        strip.add(label,BorderLayout.WEST); strip.add(search,BorderLayout.CENTER);
        return strip;
    }

    private JPanel pendingPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 5));
        JLabel guidance = new JLabel("Squads have flexible membership; role counts are guidance. Approval is required before students can join.");
        guidance.setForeground(AppTheme.MUTED);
        guidance.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 4));

        pendingModel = model(
                "Squad", "Track", "Members",
                "Synergy", "Skill Coverage", "Progress", "Risk"
        );
        pendingTable = AppTheme.emptyTable(pendingModel,"No squads are waiting for approval.");
        AppTheme.styleTable(pendingTable);
        AppTheme.styleStatusColumn(pendingTable, 6);
        AppTheme.setColumnWidths(pendingTable,165,150,115,110,150,110,100);

        JButton inspect = inspectPendingAction = button("VIEW ROSTER", BLUE);
        JButton create = button("CREATE SQUAD", AMBER);
        JButton accept = acceptPendingAction = button("ACCEPT SQUAD", GREEN);
        JButton reject = rejectPendingAction = button("REJECT SQUAD", RED);
        JButton refresh = button("REFRESH", BLUE);

        inspect.addActionListener(e -> inspectPending());
        create.addActionListener(e -> createSquad());
        accept.addActionListener(e -> decide(true));
        reject.addActionListener(e -> decide(false));
        refresh.addActionListener(e -> refreshAll());
        inspect.setEnabled(false); accept.setEnabled(false); reject.setEnabled(false);
        pendingTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) updatePendingActions(); });

        JPanel pendingContent=new JPanel(new BorderLayout(8,8)); pendingContent.setOpaque(false);
        pendingContent.add(searchStrip(pendingTable),BorderLayout.NORTH);
        pendingContent.add(scroll(pendingTable),BorderLayout.CENTER);
        p.add(pendingContent, BorderLayout.CENTER);
        p.add(guidance, BorderLayout.NORTH);
        p.add(actions(create, inspect, accept, reject, refresh),
                BorderLayout.SOUTH);
        return p;
    }

    private JPanel activePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG);

        activeModel = model(
                "Squad", "Track", "Members",
                "Synergy", "Coverage", "Progress", "Risk"
        );
        activeTable = AppTheme.emptyTable(activeModel,"No approved squads yet.");
        AppTheme.styleTable(activeTable);
        AppTheme.styleStatusColumn(activeTable, 6);
        AppTheme.setColumnWidths(activeTable,165,150,115,100,100,150,95);

        memberModel = model(
                "Registration ID", "Name", "Primary Skill",
                "Secondary Skill", "Experience"
        );
        memberTable = AppTheme.emptyTable(memberModel,"Select a squad to view its members.");
        AppTheme.styleTable(memberTable);
        AppTheme.setColumnWidths(memberTable,155,190,145,145,130);

        JPanel squadDetails = new JPanel(new BorderLayout(12, 4));
        squadDetails.setBackground(PANEL);
        squadDetails.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER,1,true),
                BorderFactory.createEmptyBorder(10,14,10,14)));
        selectedTeamSummary = new JLabel("Select a squad to see its progress.");
        selectedTeamSummary.setForeground(WHITE);
        selectedTeamSummary.setFont(new Font("SansSerif",Font.BOLD,14));
        selectedOverdue = new JLabel("0 overdue"); selectedOverdue.setForeground(AppTheme.MUTED);
        selectedProgress = new JProgressBar(0,100); selectedProgress.setStringPainted(true);
        selectedProgress.setForeground(GREEN); selectedProgress.setBackground(BG);
        JPanel summaryLabels=new JPanel(new BorderLayout()); summaryLabels.setOpaque(false);
        summaryLabels.add(selectedTeamSummary,BorderLayout.CENTER); summaryLabels.add(selectedOverdue,BorderLayout.EAST);
        squadDetails.add(summaryLabels,BorderLayout.NORTH); squadDetails.add(selectedProgress,BorderLayout.CENTER);
        JPanel activeTop=new JPanel(new BorderLayout(0,8)); activeTop.setOpaque(false);
        activeTop.add(searchStrip(activeTable),BorderLayout.NORTH);
        activeTop.add(scroll(activeTable),BorderLayout.CENTER);
        activeTop.add(squadDetails,BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, activeTop, scroll(memberTable));
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setDividerSize(10);
        split.setBackground(BG);

        activeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { selectActiveTeam(); updateActiveActions(); }
        });
        memberTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) updateActiveActions(); });

        JButton remove = removeMemberAction = button("REMOVE MEMBER", RED);
        JButton add = addMemberAction = button("ADD MEMBER", GREEN);
        JButton edit = editMemberAction = button("EDIT MEMBER", AMBER);
        JButton more = button("MORE", BLUE);

        remove.addActionListener(e -> removeStudent());
        add.addActionListener(e -> addStudent());
        edit.addActionListener(e -> editStudent());
        remove.setEnabled(false); edit.setEnabled(false); add.setEnabled(false);
        more.addActionListener(e -> {
            JPopupMenu menu=new JPopupMenu();
            JMenuItem feedbackItem=new JMenuItem("Add mentor feedback"); feedbackItem.addActionListener(event -> addFeedback()); menu.add(feedbackItem);
            JMenuItem refreshItem=new JMenuItem("Refresh squad data"); refreshItem.addActionListener(event -> refreshAll()); menu.add(refreshItem);
            AppTheme.stylePopupMenu(menu);
            menu.show(more,0,more.getHeight());
        });

        p.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        p.add(split, BorderLayout.CENTER);
        p.add(actions(add, remove, edit, more),
                BorderLayout.SOUTH);
        return p;
    }

    private JPanel milestonePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG);

        milestoneModel = model(
                "Squad", "Milestone", "Due Date", "Status", "Risk"
        );
        milestoneTable = AppTheme.emptyTable(milestoneModel,"No milestones have been created yet.");
        AppTheme.styleTable(milestoneTable);
        AppTheme.styleStatusColumn(milestoneTable, 3);
        AppTheme.styleStatusColumn(milestoneTable, 4);
        AppTheme.setColumnWidths(milestoneTable,160,235,120,130,100);

        JButton create = button("ADD MILESTONE", GREEN);
        JButton update = updateMilestoneAction = button("UPDATE STATUS", BLUE);
        JButton submissions = viewSubmissionAction = button("VIEW SUBMISSIONS", AMBER);
        JButton review = reviewWorkAction = button("REVIEW WORK", GREEN);
        JButton more = button("MORE", BLUE);

        create.addActionListener(e -> addMilestone());
        update.addActionListener(e -> updateMilestone());
        submissions.addActionListener(e -> viewSubmissions());
        review.addActionListener(e -> reviewSubmissions());
        update.setEnabled(false); submissions.setEnabled(false); review.setEnabled(false);
        milestoneTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) updateMilestoneActions(); });
        more.addActionListener(e -> {
            JPopupMenu menu=new JPopupMenu();
            JMenuItem feedbackItem=new JMenuItem("Add mentor feedback"); feedbackItem.addActionListener(event -> addFeedback()); menu.add(feedbackItem);
            JMenuItem refreshItem=new JMenuItem("Refresh milestones"); refreshItem.addActionListener(event -> refreshAll()); menu.add(refreshItem);
            AppTheme.stylePopupMenu(menu);
            menu.show(more,0,more.getHeight());
        });

        JPanel tableArea=new JPanel(new BorderLayout(8,8)); tableArea.setOpaque(false);
        tableArea.add(searchStrip(milestoneTable),BorderLayout.NORTH); tableArea.add(scroll(milestoneTable),BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(12,8,12,8));
        p.add(tableArea, BorderLayout.CENTER);
        p.add(actions(create, update, submissions, review, more),
                BorderLayout.SOUTH);
        return p;
    }

    private JPanel activityPanel() {
        JPanel p=new JPanel(new BorderLayout(10,10)); p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,8,12,8));
        activityModel=model("When","Squad","Activity","Details");
        JTable table=AppTheme.emptyTable(activityModel,"Squad milestones, submissions, and reviews will appear here."); AppTheme.styleTable(table);
        AppTheme.setColumnWidths(table,170,165,155,460);
        JLabel hint=new JLabel("Recent milestones, submissions, reviews, status changes, and mentor feedback across your squads.");
        hint.setForeground(AppTheme.MUTED);
        JButton refresh=button("REFRESH",BLUE); refresh.addActionListener(e -> loadActivity());
        JPanel body=new JPanel(new BorderLayout(8,8)); body.setOpaque(false);
        body.add(searchStrip(table),BorderLayout.NORTH); body.add(scroll(table),BorderLayout.CENTER);
        p.add(hint,BorderLayout.NORTH); p.add(body,BorderLayout.CENTER); p.add(refresh,BorderLayout.SOUTH);
        return p;
    }

    private void createSquad() {
        JTextField name = new JTextField();
        JTextField track = new JTextField();
        JSpinner frontend = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JSpinner backend = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JSpinner ai = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        JSpinner devops = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Squad name:")); form.add(name);
        form.add(new JLabel("Hackathon track:")); form.add(track);
        form.add(new JLabel("Suggested Frontend mix:")); form.add(frontend);
        form.add(new JLabel("Suggested Backend mix:")); form.add(backend);
        form.add(new JLabel("Suggested AI mix:")); form.add(ai);
        form.add(new JLabel("Suggested DevOps mix:")); form.add(devops);
        JLabel mixHint=new JLabel("Membership is unlimited; these values describe a suggested skill mix.");
        mixHint.setForeground(AppTheme.MUTED); form.add(mixHint); form.add(new JLabel(""));

        if (AppTheme.showFormDialog(this, form, "Create Squad") != JOptionPane.OK_OPTION) return;
        String squadName = name.getText().trim();
        String squadTrack = track.getText().trim();
        int f = (int) frontend.getValue(), b = (int) backend.getValue();
        int a = (int) ai.getValue(), d = (int) devops.getValue();
        if (squadName.isBlank() || squadName.length() > 100 || squadTrack.isBlank()) {
            message("Enter a squad name and hackathon track.");
            return;
        }
        if (f + b + a + d != 4) {
            message("Set the four suggested role openings to a total of 4, then try again.");
            return;
        }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int cap;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT max_squads FROM mentors WHERE mentor_id=? FOR UPDATE")) {
                    ps.setInt(1, mentorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Mentor profile not found.");
                        cap = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM teams WHERE mentor_id=? AND approval_status IN ('PENDING','APPROVED')")) {
                    ps.setInt(1, mentorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) >= cap) throw new SQLException("You have reached your squad limit.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO teams (team_name,track,mentor_id,max_frontend,max_backend,max_ai,max_devops,approval_status) " +
                                "VALUES (?,?,?,?,?,?,?,'PENDING')")) {
                    ps.setString(1, squadName);
                    ps.setString(2, squadTrack);
                    ps.setInt(3, mentorId);
                    ps.setInt(4, f);
                    ps.setInt(5, b);
                    ps.setInt(6, a);
                    ps.setInt(7, d);
                    ps.executeUpdate();
                }
                c.commit();
                message("Squad submitted for approval. Students can join once it is approved.");
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            error(e);
        }
        refreshAll();
    }

    private void loadMentor() {
        String sql = """
            SELECT mentor_id, max_squads
            FROM mentors
            WHERE user_id=?
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("Mentor profile not found.");

                mentorId = rs.getInt("mentor_id");
                maxSquads = rs.getInt("max_squads");
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void refreshAll() {
        if (mentorId == 0) return;
        loadHud();
        loadPending();
        loadActive();
        loadMilestones();
        loadActivity();
        updatePendingActions(); updateActiveActions(); updateMilestoneActions();
    }

    private void updatePendingActions() {
        if(inspectPendingAction==null) return;
        boolean selected=pendingTable.getSelectedRow()>=0;
        inspectPendingAction.setEnabled(selected); acceptPendingAction.setEnabled(selected); rejectPendingAction.setEnabled(selected);
    }

    private void updateActiveActions() {
        if(addMemberAction==null) return;
        boolean team=selectedActiveTeam>=0;
        boolean member=team && memberTable.getSelectedRow()>=0;
        addMemberAction.setEnabled(team); removeMemberAction.setEnabled(member); editMemberAction.setEnabled(member);
    }

    private void updateMilestoneActions() {
        if(updateMilestoneAction==null) return;
        boolean selected=milestoneTable.getSelectedRow()>=0;
        updateMilestoneAction.setEnabled(selected); viewSubmissionAction.setEnabled(selected); reviewWorkAction.setEnabled(selected);
    }

    private void loadHud() {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM teams
               WHERE mentor_id=? AND approval_status='APPROVED'),
              (SELECT COUNT(*) FROM teams
               WHERE mentor_id=? AND approval_status='PENDING'),
              (SELECT COUNT(*) FROM participants p
               JOIN teams t ON p.team_id=t.team_id
               WHERE t.mentor_id=?
               AND t.approval_status='APPROVED')
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, mentorId);
            ps.setInt(2, mentorId);
            ps.setInt(3, mentorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    hud.setText(String.format(
                            "Active Squads: %d / %d     |     " +
                                    "Pending Applications: %d     |     " +
                                    "Mentored Students: %d",
                            rs.getInt(1), maxSquads,
                            rs.getInt(2), rs.getInt(3)
                    ));
                }
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private static class SquadStats {
        int count, beginners, covered, milestones, completedMilestones;
        double synergy;
        String risk;
    }

    private SquadStats stats(Connection c, int teamId)
            throws SQLException {

        SquadStats s = new SquadStats();
        boolean[] coverage = new boolean[4];

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT primary_role, experience_level " +
                        "FROM participants WHERE team_id=?")) {

            ps.setInt(1, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    s.count++;

                    String role = rs.getString("primary_role");
                    String tier = rs.getString("experience_level");

                    if ("Frontend".equals(role)) coverage[0] = true;
                    if ("Backend".equals(role)) coverage[1] = true;
                    if ("AI".equals(role)) coverage[2] = true;
                    if ("DevOps".equals(role)) coverage[3] = true;

                    if ("Beginner".equals(tier)) {
                        s.beginners++;
                        s.synergy += 1.0;
                    } else if ("Intermediate".equals(tier)) {
                        s.synergy += 2.5;
                    } else {
                        s.synergy += 4.0;
                    }
                }
            }
        }

        for (boolean present : coverage)
            if (present) s.covered++;

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*),COALESCE(SUM(status='COMPLETED'),0) FROM milestones WHERE team_id=?")) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { s.milestones=rs.getInt(1); s.completedMilestones=rs.getInt(2); } }
        }

        int overdue = 0;

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM milestones " +
                        "WHERE team_id=? AND due_date<CURDATE() " +
                        "AND status<>'COMPLETED'")) {

            ps.setInt(1, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                overdue = rs.getInt(1);
            }
        }


        if (overdue >= 2) {
            s.risk = "HIGH";
        } else if (overdue == 1) {
            s.risk = "MEDIUM";
        } else {
            s.risk = "LOW";
        }

        return s;
    }

    private void loadPending() {
        pendingModel.setRowCount(0);
        pendingIds.clear();

        String sql = """
            SELECT team_id, team_name, track
            FROM teams
            WHERE mentor_id=? AND approval_status='PENDING'
            ORDER BY team_id
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, mentorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("team_id");
                    SquadStats s = stats(c, id);

                    pendingIds.add(id);
                    pendingModel.addRow(new Object[]{
                            rs.getString("team_name"),
                            rs.getString("track"),
                            s.count + " members",
                            String.format("%.1f", s.synergy),
                            s.covered + "/4",
                            s.completedMilestones + "/" + s.milestones,
                            s.risk
                    });
                }
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void loadActive() {
        activeModel.setRowCount(0);
        activeIds.clear();
        memberModel.setRowCount(0);
        memberIds.clear();
        selectedActiveTeam = -1;

        String sql = """
            SELECT team_id, team_name, track
            FROM teams
            WHERE mentor_id=? AND approval_status='APPROVED'
            ORDER BY team_id
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, mentorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("team_id");
                    SquadStats s = stats(c, id);

                    activeIds.add(id);
                    activeModel.addRow(new Object[]{
                            rs.getString("team_name"),
                            rs.getString("track"),
                            s.count + " members",
                            String.format("%.1f", s.synergy),
                            s.covered + "/4",
                            s.completedMilestones + "/" + s.milestones,
                            s.risk
                    });
                }
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private int selectedId(JTable table, List<Integer> ids) {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        int modelRow = table.convertRowIndexToModel(row);
        return ids.get(modelRow);
    }

    private void inspectPending() {
        int id = selectedId(pendingTable, pendingIds);

        if (id < 0) {
            message("Select a pending squad first.");
            return;
        }

        String sql = """
            SELECT participant_id, name,
                   primary_role, experience_level
            FROM participants WHERE team_id=?
            ORDER BY name
            """;

        StringBuilder roster = new StringBuilder();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roster.append(rs.getString("participant_id"))
                            .append(" | ")
                            .append(rs.getString("name"))
                            .append(" | ")
                            .append(rs.getString("primary_role"))
                            .append(" | ")
                            .append(rs.getString("experience_level"))
                            .append("\n");
                }
            }

            SquadStats s = stats(c, id);
            roster.append("\nSynergy: ").append(s.synergy);
            roster.append("\nSkill Coverage: ").append(s.covered);
            roster.append("/4\nRisk: ").append(s.risk);

        } catch (SQLException ex) {
            error(ex);
            return;
        }

        message(roster.toString());
    }

    private void decide(boolean accept) {
        int id = selectedId(pendingTable, pendingIds);

        if (id < 0) {
            message("Select a pending squad.");
            return;
        }

        String remark = JOptionPane.showInputDialog(
                this,
                accept ? "Approval remarks:" : "Reason for rejection:",
                accept ? "Squad Approval" : "Squad Rejection",
                JOptionPane.QUESTION_MESSAGE
        );

        if (remark == null) return;

        if (remark.length() > 500) {
            message("Remarks must be 500 characters or fewer.");
            return;
        }

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                // Serialize decisions for this mentor.
                int capacity;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT max_squads FROM mentors " +
                                "WHERE mentor_id=? FOR UPDATE")) {

                    ps.setInt(1, mentorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Mentor not found.");
                        capacity = rs.getInt(1);
                    }
                }

                String status;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT approval_status FROM teams " +
                                "WHERE team_id=? AND mentor_id=? FOR UPDATE")) {

                    ps.setInt(1, id);
                    ps.setInt(2, mentorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Squad not found.");
                        status = rs.getString(1);
                    }
                }

                if (!"PENDING".equals(status))
                    throw new SQLException("Squad is no longer pending.");

                if (accept) {
                    int approved;

                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT COUNT(*) FROM teams " +
                                    "WHERE mentor_id=? " +
                                    "AND approval_status='APPROVED'")) {

                        ps.setInt(1, mentorId);

                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            approved = rs.getInt(1);
                        }
                    }

                    if (approved >= capacity)
                        throw new SQLException("Mentor capacity reached.");
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE teams SET approval_status=?, " +
                                "mentor_remarks=? WHERE team_id=? " +
                                "AND mentor_id=? AND approval_status='PENDING'")) {

                    ps.setString(1, accept ? "APPROVED" : "REJECTED");
                    ps.setString(2, remark);
                    ps.setInt(3, id);
                    ps.setInt(4, mentorId);

                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Decision could not be saved.");
                }

                ActivitySupport.record(c,id,userId,"SQUAD_STATUS",
                        accept?"Squad approved.":"Squad rejected: "+remark);

                c.commit();
                message(accept ? "Squad approved." : "Squad rejected.");

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }

        } catch (SQLException ex) {
            error(ex);
        }

        refreshAll();
    }

    private void selectActiveTeam() {
        int id = selectedId(activeTable, activeIds);
        selectedActiveTeam = id;

        memberModel.setRowCount(0);
        memberIds.clear();

        if (id < 0) {
            selectedTeamSummary.setText("Select a squad to see its progress.");
            selectedProgress.setValue(0); selectedProgress.setString("No squad selected");
            selectedOverdue.setText("0 overdue"); return;
        }
        int selectedRow=activeTable.getSelectedRow();
        if(selectedRow>=0) {
            int modelRow=activeTable.convertRowIndexToModel(selectedRow);
            selectedTeamSummary.setText(activeModel.getValueAt(modelRow,0)+"  ·  "+
                    activeModel.getValueAt(modelRow,1)+"  ·  "+activeModel.getValueAt(modelRow,2)+
                    "  ·  Skills "+activeModel.getValueAt(modelRow,4));
        }
        updateSelectedSquadProgress(id);

        String sql = """
            SELECT participant_id, name, primary_role,
                   secondary_role, experience_level
            FROM participants WHERE team_id=?
            ORDER BY name
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    memberIds.add(rs.getString("participant_id"));

                    memberModel.addRow(new Object[]{
                            rs.getString("participant_id"),
                            rs.getString("name"),
                            rs.getString("primary_role"),
                            rs.getString("secondary_role"),
                            rs.getString("experience_level")
                    });
                }
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void updateSelectedSquadProgress(int teamId) {
        String sql="SELECT COUNT(*),COALESCE(SUM(status='COMPLETED'),0),"+
                "COALESCE(SUM(due_date<CURDATE() AND status<>'COMPLETED'),0) FROM milestones WHERE team_id=?";
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setInt(1,teamId); try(ResultSet rs=ps.executeQuery()) { if(rs.next()) {
                int total=rs.getInt(1),complete=rs.getInt(2),overdue=rs.getInt(3);
                int percent=total==0?0:complete*100/total;
                selectedProgress.setValue(percent);
                selectedProgress.setString(total==0?"No milestones yet":complete+" of "+total+" complete · "+percent+"%");
                selectedOverdue.setText(overdue+" overdue");
            } }
        } catch(SQLException ex) { error(ex); }
    }

    private void loadActivity() {
        if(activityModel==null) return;
        activityModel.setRowCount(0);
        String sql="SELECT e.created_at,t.team_name,e.event_type,e.description,u.full_name FROM activity_events e "+
                "JOIN teams t ON t.team_id=e.team_id LEFT JOIN users u ON u.user_id=e.actor_user_id "+
                "WHERE t.mentor_id=? ORDER BY e.created_at DESC LIMIT 200";
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setInt(1,mentorId); try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) activityModel.addRow(new Object[]{rs.getTimestamp(1),rs.getString(2),rs.getString(3),
                        (rs.getString(5)==null?"SkillForge":rs.getString(5))+": "+rs.getString(4)});
            }
        } catch(SQLException ex) { error(ex); }
    }

    private String selectedMember() {
        int row = memberTable.getSelectedRow();
        if (row < 0) return null;

        return memberIds.get(
                memberTable.convertRowIndexToModel(row)
        );
    }

    private static class StudentOption {
        final String participantId;
        final String name;
        final String role;
        final String experience;

        StudentOption(String participantId, String name,
                      String role, String experience) {
            this.participantId = participantId;
            this.name = name;
            this.role = role;
            this.experience = experience;
        }

        @Override
        public String toString() {
            return name + " — " + role + " — " + experience
                    + " (" + participantId + ")";
        }
    }

    private void addStudent() {
        if (selectedActiveTeam < 0) {
            message("Select one of your active squads first.");
            return;
        }

        JComboBox<StudentOption> students = new JComboBox<>();
        String sql = "SELECT participant_id, name, primary_role, " +
                "experience_level FROM participants WHERE team_id IS NULL " +
                "ORDER BY name";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.addItem(new StudentOption(
                        rs.getString("participant_id"),
                        rs.getString("name"),
                        rs.getString("primary_role"),
                        rs.getString("experience_level")
                ));
            }
        } catch (SQLException ex) {
            error(ex);
            return;
        }

        if (students.getItemCount() == 0) {
            message("There are no unassigned students available to add.");
            return;
        }

        JPanel form = new JPanel(new GridLayout(2, 1, 8, 8));
        form.add(new JLabel("Choose an unassigned student to add to this squad:"));
        form.add(students);
        if (AppTheme.showFormDialog(this, form, "Add Student to Squad")
                != JOptionPane.OK_OPTION) return;

        StudentOption chosen = (StudentOption) students.getSelectedItem();
        if (chosen == null) return;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int quota;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT max_frontend, max_backend, max_ai, max_devops " +
                                "FROM teams WHERE team_id=? AND mentor_id=? " +
                                "AND approval_status='APPROVED' FOR UPDATE")) {
                    ps.setInt(1, selectedActiveTeam);
                    ps.setInt(2, mentorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Squad not found or not managed by you.");
                        quota = switch (chosen.role) {
                            case "Frontend" -> rs.getInt("max_frontend");
                            case "Backend" -> rs.getInt("max_backend");
                            case "AI" -> rs.getInt("max_ai");
                            case "DevOps" -> rs.getInt("max_devops");
                            default -> 0;
                        };
                    }
                }

                String currentRole = null;
                String currentExperience = null;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT primary_role, experience_level, team_id " +
                                "FROM participants WHERE participant_id=? FOR UPDATE")) {
                    ps.setString(1, chosen.participantId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Student record not found.");
                        if (rs.getObject("team_id") != null)
                            throw new SQLException("This student has already joined a squad.");
                        currentRole = rs.getString("primary_role");
                        currentExperience = rs.getString("experience_level");
                    }
                }

                int count = 0, roleCount = 0, beginners = 0;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT primary_role, experience_level FROM participants " +
                                "WHERE team_id=?")) {
                    ps.setInt(1, selectedActiveTeam);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            count++;
                            if (java.util.Objects.equals(
                                    currentRole, rs.getString("primary_role"))) roleCount++;
                            if ("Beginner".equals(rs.getString("experience_level"))) beginners++;
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE participants SET team_id=? " +
                                "WHERE participant_id=? AND team_id IS NULL")) {
                    ps.setInt(1, selectedActiveTeam);
                    ps.setString(2, chosen.participantId);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Could not add the student; refresh and try again.");
                }
                ActivitySupport.record(c,selectedActiveTeam,userId,"MEMBER_ADDED","Added "+chosen.name+" to the squad.");
                c.commit();
                message(chosen.name + " added to the squad.");
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            error(ex);
        }
        refreshAll();
    }

    private void removeStudent() {
        String participant = selectedMember();

        if (participant == null || selectedActiveTeam < 0) {
            message("Select an active squad and student.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this, "Remove student " + participant + " from squad?",
                "Confirm Removal", JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = """
            UPDATE participants SET team_id=NULL
            WHERE participant_id=? AND team_id=?
            AND EXISTS (
                SELECT 1 FROM teams
                WHERE team_id=? AND mentor_id=?
                AND approval_status='APPROVED'
            )
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, participant);
            ps.setInt(2, selectedActiveTeam);
            ps.setInt(3, selectedActiveTeam);
            ps.setInt(4, mentorId);

            if (ps.executeUpdate() == 1) {
                ActivitySupport.record(c,selectedActiveTeam,userId,"MEMBER_REMOVED","Removed a student from the squad.");
                message("Student removed successfully.");
            } else
                message("Student could not be removed.");

        } catch (SQLException ex) {
            error(ex);
        }

        refreshAll();
    }

    private void editStudent() {
        String participant = selectedMember();

        if (participant == null || selectedActiveTeam < 0) {
            message("Select a student first.");
            return;
        }

        String[] roles = {"Frontend", "Backend", "AI", "DevOps"};
        String[] levels = {"Beginner", "Intermediate", "Advanced"};

        JComboBox<String> primary = new JComboBox<>(roles);
        JComboBox<String> secondary = new JComboBox<>(roles);
        JComboBox<String> level = new JComboBox<>(levels);

        int row = memberTable.getSelectedRow();
        primary.setSelectedItem(memberTable.getValueAt(row, 2));
        secondary.setSelectedItem(memberTable.getValueAt(row, 3));
        level.setSelectedItem(memberTable.getValueAt(row, 4));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Primary Role"));
        form.add(primary);
        form.add(new JLabel("Secondary Role"));
        form.add(secondary);
        form.add(new JLabel("Experience"));
        form.add(level);

        int result = AppTheme.showFormDialog(this, form, "Edit Student");

        if (result != JOptionPane.OK_OPTION) return;

        String newRole = (String) primary.getSelectedItem();
        String newLevel = (String) level.getSelectedItem();

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                // Lock the team before checking quotas.
                int quota;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT max_frontend, max_backend, max_ai, " +
                                "max_devops FROM teams WHERE team_id=? " +
                                "AND mentor_id=? AND approval_status='APPROVED' " +
                                "FOR UPDATE")) {

                    ps.setInt(1, selectedActiveTeam);
                    ps.setInt(2, mentorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Unauthorized squad.");

                        quota = switch (newRole) {
                            case "Frontend" -> rs.getInt("max_frontend");
                            case "Backend" -> rs.getInt("max_backend");
                            case "AI" -> rs.getInt("max_ai");
                            default -> rs.getInt("max_devops");
                        };
                    }
                }

                int roleCount = 0;
                int beginnerCount = 0;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT participant_id, primary_role, " +
                                "experience_level FROM participants " +
                                "WHERE team_id=?")) {

                    ps.setInt(1, selectedActiveTeam);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (participant.equals(
                                    rs.getString("participant_id")))
                                continue;

                            if (newRole.equals(rs.getString("primary_role")))
                                roleCount++;

                            if ("Beginner".equals(
                                    rs.getString("experience_level")))
                                beginnerCount++;
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE participants SET primary_role=?, " +
                                "secondary_role=?, experience_level=? " +
                                "WHERE participant_id=? AND team_id=?")) {

                    ps.setString(1, newRole);
                    ps.setString(2, (String) secondary.getSelectedItem());
                    ps.setString(3, newLevel);
                    ps.setString(4, participant);
                    ps.setInt(5, selectedActiveTeam);

                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Student update failed.");
                }

                c.commit();
                message("Student updated successfully.");

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            error(ex);
        }

        refreshAll();
    }

    private JComboBox<TeamOption> teamChoices() {
        JComboBox<TeamOption> box = new JComboBox<>();

        String sql = """
            SELECT team_id, team_name FROM teams
            WHERE mentor_id=? AND approval_status='APPROVED'
            ORDER BY team_name
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, mentorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    box.addItem(new TeamOption(
                            rs.getInt("team_id"),
                            rs.getString("team_name")
                    ));
                }
            }
        } catch (SQLException ex) {
            error(ex);
        }

        return box;
    }

    private List<TeamOption> teamOptions() {
        List<TeamOption> options = new ArrayList<>();
        String sql="SELECT team_id,team_name FROM teams WHERE mentor_id=? AND approval_status='APPROVED' ORDER BY team_name";
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setInt(1,mentorId); try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) options.add(new TeamOption(rs.getInt(1),rs.getString(2)));
            }
        } catch(SQLException ex) { error(ex); }
        return options;
    }

    private void viewSubmissions() {
        int row=milestoneTable.getSelectedRow();
        if(row<0) { message("Select a milestone first."); return; }
        int id=milestoneIds.get(milestoneTable.convertRowIndexToModel(row));
        DefaultTableModel submissions=model("Student","Submitted","Link","File","Review","Feedback");
        List<String> fileNames=new ArrayList<>();
        List<byte[]> fileBytes=new ArrayList<>();
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(
                "SELECT p.name,s.submitted_at,s.submission_link,s.file_name,s.file_data,s.review_status,s.review_feedback FROM milestone_submissions s " +
                "JOIN participants p ON p.participant_id=s.participant_id JOIN milestones ms ON ms.milestone_id=s.milestone_id " +
                "JOIN teams t ON t.team_id=ms.team_id WHERE s.milestone_id=? AND t.mentor_id=? ORDER BY p.name")) {
            ps.setInt(1,id); ps.setInt(2,mentorId);
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) {
                submissions.addRow(new Object[]{rs.getString(1),rs.getTimestamp(2),rs.getString(3),rs.getString(4),rs.getString(6),rs.getString(7)});
                fileNames.add(rs.getString(4)); fileBytes.add(rs.getBytes(5));
            } }
        } catch(SQLException ex) { error(ex); return; }
        JTable table=new JTable(submissions); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        AppTheme.styleTable(table); AppTheme.styleStatusColumn(table,4);
        if(submissions.getRowCount()==0) { message("No student submissions yet."); return; }
        int choice=JOptionPane.showConfirmDialog(this,new JScrollPane(table),"Student Submissions — select a row, then press OK to open its link or file",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(choice!=JOptionPane.OK_OPTION || table.getSelectedRow()<0) return;
        int selected=table.convertRowIndexToModel(table.getSelectedRow());
        String link=(String)submissions.getValueAt(selected,2);
        try {
            if(link!=null && !link.isBlank()) { java.awt.Desktop.getDesktop().browse(java.net.URI.create(link)); return; }
            byte[] bytes=fileBytes.get(selected); String name=fileNames.get(selected);
            if(bytes==null) { message("This submission has no attached link or file."); return; }
            File output=File.createTempFile("skillforge-submission-", "-"+name);
            Files.write(output.toPath(),bytes); java.awt.Desktop.getDesktop().open(output);
        } catch(Exception ex) { error(ex); }
    }

    private void reviewSubmissions() {
        int row=milestoneTable.getSelectedRow();
        if(row<0) { message("Select a milestone first."); return; }
        int modelRow=milestoneTable.convertRowIndexToModel(row);
        int milestoneId=milestoneIds.get(modelRow),teamId=milestoneTeamIds.get(modelRow);
        DefaultTableModel model=model("Student","Submitted","Current Review","Feedback");
        List<Integer> submissionIds=new ArrayList<>();
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(
                "SELECT s.submission_id,p.name,s.submitted_at,s.review_status,s.review_feedback " +
                "FROM milestone_submissions s JOIN participants p ON p.participant_id=s.participant_id " +
                "JOIN milestones m ON m.milestone_id=s.milestone_id JOIN teams t ON t.team_id=m.team_id " +
                "WHERE s.milestone_id=? AND t.mentor_id=? ORDER BY s.submitted_at DESC")) {
            ps.setInt(1,milestoneId); ps.setInt(2,mentorId);
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) {
                submissionIds.add(rs.getInt(1));
                model.addRow(new Object[]{rs.getString(2),rs.getTimestamp(3),rs.getString(4),rs.getString(5)});
            } }
        } catch(SQLException ex) { error(ex); return; }
        if(submissionIds.isEmpty()) { message("No student submissions to review yet."); return; }
        JTable table=new JTable(model); AppTheme.styleTable(table); AppTheme.styleStatusColumn(table,2);
        if(JOptionPane.showConfirmDialog(this,new JScrollPane(table),"Choose a submission to review",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION || table.getSelectedRow()<0) return;
        int selected=table.convertRowIndexToModel(table.getSelectedRow());
        JComboBox<String> status=new JComboBox<>(new String[]{"APPROVED","NEEDS_CHANGES"});
        JTextArea feedback=new JTextArea(5,32); feedback.setLineWrap(true); feedback.setWrapStyleWord(true);
        Object previous=model.getValueAt(selected,3); if(previous!=null) feedback.setText(previous.toString());
        JPanel form=new JPanel(new BorderLayout(8,8));
        JPanel statusRow=new JPanel(new GridLayout(1,2,8,8)); statusRow.add(new JLabel("Review decision")); statusRow.add(status);
        form.add(statusRow,BorderLayout.NORTH); form.add(new JScrollPane(feedback),BorderLayout.CENTER);
        if(AppTheme.showFormDialog(this,form,"Review Student Work")!=JOptionPane.OK_OPTION) return;
        String decision=(String)status.getSelectedItem();
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(
                "UPDATE milestone_submissions s JOIN milestones m ON m.milestone_id=s.milestone_id " +
                "JOIN teams t ON t.team_id=m.team_id SET s.review_status=?,s.review_feedback=? " +
                "WHERE s.submission_id=? AND s.milestone_id=? AND t.mentor_id=?")) {
            ps.setString(1,decision); ps.setString(2,feedback.getText().trim());
            ps.setInt(3,submissionIds.get(selected)); ps.setInt(4,milestoneId); ps.setInt(5,mentorId);
            if(ps.executeUpdate()!=1) throw new SQLException("Review could not be saved.");
            ActivitySupport.record(c,teamId,userId,"SUBMISSION_REVIEW","Reviewed student work: "+decision+".");
            message("Review saved. Students can resubmit work marked NEEDS_CHANGES.");
        } catch(SQLException ex) { error(ex); }
        refreshAll();
    }

    private static class TeamOption {
        final int id;
        final String name;

        TeamOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void addFeedback() {
        JComboBox<TeamOption> teams = teamChoices();

        if (teams.getItemCount() == 0) {
            message("No approved squads available.");
            return;
        }

        JTextArea feedback = new JTextArea(5, 30);
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(teams, BorderLayout.NORTH);
        form.add(new JScrollPane(feedback), BorderLayout.CENTER);

        int result = AppTheme.showFormDialog(this, form, "Mentor Feedback");

        if (result != JOptionPane.OK_OPTION) return;

        TeamOption team = (TeamOption) teams.getSelectedItem();
        String text = feedback.getText().trim();

        if (team == null || text.isEmpty()) {
            message("Enter feedback.");
            return;
        }

        String sql = """
            INSERT INTO mentor_feedback
            (team_id, mentor_id, feedback)
            SELECT team_id, ?, ?
            FROM teams
            WHERE team_id=? AND mentor_id=?
            AND approval_status='APPROVED'
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, mentorId);
            ps.setString(2, text);
            ps.setInt(3, team.id);
            ps.setInt(4, mentorId);

            if (ps.executeUpdate() != 1)
                throw new SQLException("Feedback could not be saved.");
            ActivitySupport.record(c, team.id, userId, "MENTOR_FEEDBACK", "Added mentor feedback.");

            message("Feedback saved.");

        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void addMilestone() {
        List<TeamOption> teams = teamOptions();
        if (teams.isEmpty()) {
            message("No approved squads available.");
            return;
        }
        JPanel targetsPanel = new JPanel(new GridLayout(0, 1));
        JCheckBox all = new JCheckBox("All my approved squads"); targetsPanel.add(all);
        List<JCheckBox> checks = new ArrayList<>();
        for (TeamOption team : teams) { JCheckBox check = new JCheckBox(team.name); checks.add(check); targetsPanel.add(check); }
        all.addActionListener(e -> checks.forEach(check -> check.setSelected(all.isSelected())));

        JTextField title = new JTextField();
        JTextField description = new JTextField();
        JTextField dueDate = new JTextField(
                LocalDate.now().plusDays(7).toString()
        );
        JTextField resourceLink = new JTextField();
        JLabel fileLabel = new JLabel("No file selected");
        File[] attachment = {null};
        JButton chooseFile = button("CHOOSE FILE", BLUE);
        chooseFile.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File picked = chooser.getSelectedFile();
                if (picked.length() > 10L * 1024 * 1024) { message("Maximum file size is 10 MB."); return; }
                attachment[0] = picked; fileLabel.setText(picked.getName());
            }
        });

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Teams (select one, several, or all)")); form.add(new JScrollPane(targetsPanel));
        form.add(new JLabel("Milestone Title"));
        form.add(title);
        form.add(new JLabel("Description"));
        form.add(description);
        form.add(new JLabel("Due Date (YYYY-MM-DD)"));
        form.add(dueDate);
        form.add(new JLabel("Resource link (optional)")); form.add(resourceLink);
        form.add(chooseFile); form.add(fileLabel);
        JLabel fileHint=new JLabel("Attachments may be up to 10 MB. Set a due date in YYYY-MM-DD format.");
        fileHint.setForeground(AppTheme.MUTED); form.add(fileHint); form.add(new JLabel(""));

        int result = AppTheme.showFormDialog(this, AppTheme.formSection("MILESTONE DETAILS",form), "Create Milestone");

        if (result != JOptionPane.OK_OPTION) return;

        List<TeamOption> selectedTeams = new ArrayList<>();
        for (int i=0;i<teams.size();i++) if(checks.get(i).isSelected()) selectedTeams.add(teams.get(i));
        if (selectedTeams.isEmpty() || title.getText().isBlank()) {
            message("Choose at least one squad and enter a milestone title.");
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dueDate.getText().trim());

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO milestones(team_id,title,description,due_date,status,attachment_name,attachment,resource_link) " +
                    "SELECT team_id,?,?,?,'TODO',?,?,? FROM teams WHERE team_id=? AND mentor_id=? AND approval_status='APPROVED'")) {
                for (TeamOption team : selectedTeams) {
                    ps.setString(1,title.getText().trim()); ps.setString(2,description.getText().trim());
                    ps.setDate(3,Date.valueOf(date)); ps.setString(4,attachment[0]==null?null:attachment[0].getName());
                    if(attachment[0]==null) ps.setNull(5,Types.BLOB); else ps.setBytes(5,Files.readAllBytes(attachment[0].toPath()));
                    ps.setString(6,resourceLink.getText().trim().isEmpty()?null:resourceLink.getText().trim());
                    ps.setInt(7,team.id); ps.setInt(8,mentorId); ps.addBatch();
                }
                int[] inserted=ps.executeBatch();
                if(inserted.length!=selectedTeams.size()) throw new SQLException("Some milestones could not be created.");
                for(TeamOption team:selectedTeams) ActivitySupport.record(c,team.id,userId,"MILESTONE_CREATED",
                        "Created milestone: "+title.getText().trim());
                message("Milestone assigned to " + selectedTeams.size() + " squad(s).");
            }

        } catch (Exception ex) {
            error(ex);
        }

        refreshAll();
    }

    private void loadMilestones() {
        milestoneModel.setRowCount(0);
        milestoneIds.clear();
        milestoneTeamIds.clear();

        String sql = """
            SELECT ms.milestone_id, t.team_name,
                   ms.title, ms.due_date, ms.status,
                   t.team_id
            FROM milestones ms
            JOIN teams t ON ms.team_id=t.team_id
            WHERE t.mentor_id=?
            AND t.approval_status='APPROVED'
            ORDER BY ms.due_date
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, mentorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("team_id");
                    SquadStats s = stats(c, id);

                    milestoneIds.add(rs.getInt("milestone_id"));
                    milestoneTeamIds.add(id);

                    milestoneModel.addRow(new Object[]{
                            rs.getString("team_name"),
                            rs.getString("title"),
                            rs.getDate("due_date"),
                            rs.getString("status"),
                            s.risk
                    });
                }
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void updateMilestone() {
        int row = milestoneTable.getSelectedRow();

        if (row < 0) {
            message("Select a milestone first.");
            return;
        }

        int id = milestoneIds.get(
                milestoneTable.convertRowIndexToModel(row)
        );

        String[] statuses = {
                "TODO", "IN_PROGRESS", "COMPLETED"
        };

        String selected = (String) JOptionPane.showInputDialog(
                this, "Choose milestone status:",
                "Update Milestone",
                JOptionPane.QUESTION_MESSAGE,
                null, statuses, statuses[0]
        );

        if (selected == null) return;

        String sql = """
            UPDATE milestones ms
            JOIN teams t ON ms.team_id=t.team_id
            SET ms.status=?
            WHERE ms.milestone_id=? AND t.mentor_id=?
            AND t.approval_status='APPROVED'
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, selected);
            ps.setInt(2, id);
            ps.setInt(3, mentorId);

            if (ps.executeUpdate() != 1)
                throw new SQLException("Milestone update failed.");
            int modelRow=milestoneTable.convertRowIndexToModel(row);
            ActivitySupport.record(c,milestoneTeamIds.get(modelRow),userId,"MILESTONE_STATUS",
                    "Updated milestone status to "+selected+".");

            message("Milestone status updated.");

        } catch (SQLException ex) {
            error(ex);
        }

        refreshAll();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(
                this, ex.getMessage(),
                "SkillForge Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}

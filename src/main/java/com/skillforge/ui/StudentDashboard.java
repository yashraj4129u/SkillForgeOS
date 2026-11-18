
package com.skillforge.ui;

import com.skillforge.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.io.File;
import java.nio.file.Files;
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

public class StudentDashboard extends JFrame {

    private final int userId;
    private final Color bg = AppTheme.BACKGROUND;
    private final Color panel = AppTheme.SURFACE;
    private final Color green = AppTheme.GREEN;
    private final Color blue = AppTheme.BLUE;
    private final Color red = AppTheme.RED;
    private final Color amber = AppTheme.AMBER;
    private final Color white = AppTheme.TEXT;

    private JLabel profileLabel;
    private DefaultTableModel squadModel;
    private DefaultTableModel milestoneModel;
    private JTable squadTable;
    private JTable milestoneTable;
    private JLabel progressLabel;
    private JLabel overdueLabel;
    private JProgressBar progressBar;
    private JComboBox<String> trackFilter, mentorFilter, skillFilter;
    private TableRowSorter<DefaultTableModel> squadSorter;
    private DefaultTableModel activityModel;
    private JButton joinAction, leaveAction, membersAction, autoAction;
    private JButton submitAction, resourceAction;
    private JMenuItem membersMenuItem;
    private final List<Integer> milestoneIds = new ArrayList<>();
    private final List<Integer> squadIds = new ArrayList<>();
    private SwingWorker<List<Object[]>, Void> squadLoader;

    private String primaryRole;
    private String experience;
    private Integer currentTeam;

    public StudentDashboard(int userId) {
        this.userId = userId;

        setTitle("SkillForge OS - Student Dashboard");
        setSize(1240, 820);
        setMinimumSize(new Dimension(1000, 660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(bg);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JButton logout = button("LOGOUT", red);
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        root.add(AppTheme.header("Student Portal",
                "Your profile, squad opportunities, and project milestones", logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("My Profile", profilePanel());
        tabs.addTab("Discover Squads", squadsPanel());
        tabs.addTab("My Milestones", milestonesPanel());
        tabs.addTab("Squad Activity", activityPanel());
        AppTheme.styleTabs(tabs);

        root.add(tabs, BorderLayout.CENTER);
        add(root);

        refreshAll();
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        AppTheme.styleButton(b, color);
        return b;
    }

    private JComboBox<String> styleFilter(JComboBox<String> combo) {
        AppTheme.styleComboBox(combo);
        combo.setPreferredSize(new Dimension(200,40));
        return combo;
    }

    private JPanel filterLabel(String label, JComboBox<String> combo) {
        JPanel box = new JPanel(); box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel(label); caption.setForeground(AppTheme.MUTED);
        caption.setFont(new Font("SansSerif", Font.BOLD, 10));
        caption.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        box.add(caption); box.add(combo); return box;
    }

    private void refreshSquadFilterChoices() {
        if (trackFilter == null) return;
        populateFilter(trackFilter, 1, "All tracks");
        populateFilter(mentorFilter, 2, "All mentors");
        skillFilter.removeAllItems(); skillFilter.addItem("Any skill");
        for (String skill : new String[]{"Frontend", "Backend", "AI", "DevOps"}) skillFilter.addItem(skill);
        applySquadFilters();
    }

    private void populateFilter(JComboBox<String> combo, int column, String allLabel) {
        Object previous = combo.getSelectedItem();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int row = 0; row < squadModel.getRowCount(); row++) {
            Object value = squadModel.getValueAt(row, column);
            if (value != null && !value.toString().isBlank()) values.add(value.toString());
        }
        combo.removeAllItems(); combo.addItem(allLabel);
        values.forEach(combo::addItem);
        if (previous != null) combo.setSelectedItem(previous);
        if (combo.getSelectedIndex() < 0) combo.setSelectedIndex(0);
    }

    private void applySquadFilters() {
        if (squadSorter == null) return;
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String track = (String) trackFilter.getSelectedItem();
        String mentor = (String) mentorFilter.getSelectedItem();
        String skill = (String) skillFilter.getSelectedItem();
        if (track != null && !track.startsWith("All ")) filters.add(RowFilter.regexFilter("^" + Pattern.quote(track) + "$", 1));
        if (mentor != null && !mentor.startsWith("All ")) filters.add(RowFilter.regexFilter("^" + Pattern.quote(mentor) + "$", 2));
        if (skill != null && !skill.startsWith("Any ")) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(skill), 5));
        squadSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private JPanel profilePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(22, 18, 18, 18));

        profileLabel = new JLabel("Loading profile...");
        profileLabel.setForeground(white);
        profileLabel.setFont(new Font("SansSerif", Font.PLAIN, 17));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(panel);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(28, 30, 28, 30)));
        card.add(profileLabel);

        JButton refresh = button("REFRESH", blue);
        refresh.addActionListener(e -> refreshAll());
        JButton export = button("EXPORT PROFILE", green);
        export.addActionListener(e -> exportPortfolio());
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 12));
        actions.setOpaque(false);
        actions.add(export); actions.add(refresh);

        p.add(card, BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private JPanel squadsPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));

        squadModel = new DefaultTableModel(
                new String[]{
                        "Squad", "Track", "Mentor", "Members",
                        "Synergy", "Skill Mix", "Status"
                }, 0
        ) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        squadTable = AppTheme.emptyTable(squadModel,"No squads match these filters yet.");
        AppTheme.styleTable(squadTable);
        AppTheme.styleStatusColumn(squadTable, 6);
        squadSorter = new TableRowSorter<>(squadModel);
        squadTable.setRowSorter(squadSorter);
        AppTheme.setColumnWidths(squadTable,145,170,155,105,95,245,120);
        squadTable.setRowHeight(32);
        squadTable.setBackground(panel);
        squadTable.setForeground(white);
        squadTable.setSelectionBackground(blue);
        squadTable.setGridColor(bg);
        squadTable.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 14)
        );

        squadTable.getTableHeader().setPreferredSize(
                new Dimension(0, 40)
        );

        squadTable.setFont(
                new Font("SansSerif", Font.PLAIN, 14)
        );

        squadTable.setShowVerticalLines(false);
        squadTable.setSelectionForeground(Color.WHITE);

        JScrollPane scrollPane = AppTheme.styleScrollPane(squadTable);

        JPanel guidance = new JPanel(new BorderLayout(8, 6));
        guidance.setOpaque(false);
        JLabel hint = new JLabel("Browse approved squads and filter by track, mentor, or skill. Membership is flexible.");
        hint.setForeground(AppTheme.MUTED);
        JPanel filters = new JPanel(new GridLayout(1, 3, 10, 0));
        filters.setOpaque(false);
        trackFilter = new JComboBox<>(); mentorFilter = new JComboBox<>(); skillFilter = new JComboBox<>();
        styleFilter(trackFilter); styleFilter(mentorFilter); styleFilter(skillFilter);
        filters.add(filterLabel("TRACK", trackFilter));
        filters.add(filterLabel("MENTOR", mentorFilter));
        filters.add(filterLabel("SKILL", skillFilter));
        guidance.add(hint, BorderLayout.NORTH); guidance.add(filters, BorderLayout.CENTER);
        java.awt.event.ActionListener filterAction = e -> applySquadFilters();
        trackFilter.addActionListener(filterAction); mentorFilter.addActionListener(filterAction); skillFilter.addActionListener(filterAction);
        guidance.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 4));
        p.add(guidance, BorderLayout.NORTH);
        p.add(scrollPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 4, 12, 12));
        actions.setBackground(bg);
        actions.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JButton join = joinAction = button("JOIN", green);
        JButton leave = leaveAction = button("LEAVE", red);
        JButton auto = autoAction = button("AUTO MATCH", blue);
        JButton membersButton = membersAction = button("MORE", amber);
        join.addActionListener(e -> joinSelected());
        leave.addActionListener(e -> leaveSquad());
        auto.addActionListener(e -> autoAssign());
        membersButton.addActionListener(e -> {
            JPopupMenu menu=new JPopupMenu();
            JMenuItem create=new JMenuItem("Create a new squad"); create.addActionListener(event -> createSquad()); menu.add(create);
            membersMenuItem=new JMenuItem("View squad members"); membersMenuItem.addActionListener(event -> viewMembers()); menu.add(membersMenuItem);
            JMenuItem refresh=new JMenuItem("Refresh squad list"); refresh.addActionListener(event -> refreshAll()); menu.add(refresh);
            AppTheme.stylePopupMenu(menu);
            menu.show(membersButton,0,membersButton.getHeight());
        });
        join.setEnabled(false); leave.setEnabled(false); auto.setEnabled(false);
        squadTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) updateSquadActions(); });

        actions.add(join);
        actions.add(leave);
        actions.add(auto);
        actions.add(membersButton);

        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private JPanel milestonesPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(bg);

        milestoneModel = new DefaultTableModel(
                new String[]{"Milestone", "Due Date", "Status", "Reminder", "Submission", "Review", "Mentor Feedback"}, 0
        ) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        milestoneTable = AppTheme.emptyTable(milestoneModel,"No milestones have been assigned to your squad yet.");
        AppTheme.styleTable(milestoneTable);
        AppTheme.setColumnWidths(milestoneTable,210,105,115,100,130,140,280);
        AppTheme.styleStatusColumn(milestoneTable, 2);
        AppTheme.styleStatusColumn(milestoneTable, 3);
        AppTheme.styleStatusColumn(milestoneTable, 5);
        progressLabel = new JLabel("Squad progress: loading...");
        progressLabel.setForeground(white);
        progressLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        progressLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));
        overdueLabel = new JLabel("0 overdue"); overdueLabel.setForeground(AppTheme.MUTED);
        progressBar = new JProgressBar(0, 100); progressBar.setStringPainted(true);
        progressBar.setForeground(green); progressBar.setBackground(panel);
        JPanel progress = new JPanel(new BorderLayout(14, 8)); progress.setOpaque(false);
        progress.add(progressLabel, BorderLayout.WEST); progress.add(progressBar, BorderLayout.CENTER);
        progress.add(overdueLabel, BorderLayout.EAST);

        JButton refresh = button("REFRESH", blue);
        refresh.addActionListener(e -> refreshAll());
        JButton submit = submitAction = button("SUBMIT WORK", green);
        submit.addActionListener(e -> submitMilestone());
        JButton resource = resourceAction = button("OPEN RESOURCE", blue);
        resource.addActionListener(e -> openMilestoneResource());
        submit.setEnabled(false); resource.setEnabled(false);
        milestoneTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) updateMilestoneActions(); });

        p.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        p.add(progress, BorderLayout.NORTH);
        p.add(AppTheme.styleScrollPane(milestoneTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 12, 12));
        actions.setBackground(bg);
        actions.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        actions.add(submit); actions.add(resource); actions.add(refresh);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private JPanel activityPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10)); p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        activityModel = new DefaultTableModel(new String[]{"When", "Activity", "Details"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = AppTheme.emptyTable(activityModel,"Your squad activity will appear here."); AppTheme.styleTable(table);
        AppTheme.setColumnWidths(table,180,160,520);
        JLabel hint = new JLabel("Recent squad milestones, submissions, reviews, and mentor feedback.");
        hint.setForeground(AppTheme.MUTED);
        JButton refresh = button("REFRESH", blue); refresh.addActionListener(e -> loadActivity());
        p.add(hint, BorderLayout.NORTH); p.add(AppTheme.styleScrollPane(table), BorderLayout.CENTER);
        p.add(refresh, BorderLayout.SOUTH); return p;
    }

    private void loadActivity() {
        if (activityModel == null) return;
        activityModel.setRowCount(0);
        if (currentTeam == null) return;
        String sql = "SELECT e.created_at, e.event_type, e.description, u.full_name " +
                "FROM activity_events e LEFT JOIN users u ON u.user_id=e.actor_user_id " +
                "WHERE e.team_id=? ORDER BY e.created_at DESC LIMIT 100";
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setInt(1,currentTeam); try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) activityModel.addRow(new Object[]{rs.getTimestamp(1),
                        rs.getString(2), (rs.getString(4)==null?"SkillForge":rs.getString(4))+": "+rs.getString(3)});
            }
        } catch(SQLException ex) { error(ex); }
    }

    private void exportPortfolio() {
        String name, primary, secondary, level, link, team;
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(
                "SELECT p.name,p.primary_role,p.secondary_role,p.experience_level,p.github_url,t.team_name " +
                "FROM participants p LEFT JOIN teams t ON t.team_id=p.team_id WHERE p.user_id=?")) {
            ps.setInt(1,userId); try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new SQLException("Student profile not found.");
                name=rs.getString(1); primary=rs.getString(2); secondary=rs.getString(3);
                level=rs.getString(4); link=rs.getString(5); team=rs.getString(6);
            }
            StringBuilder completed=new StringBuilder();
            if(currentTeam!=null) try(PreparedStatement milestones=c.prepareStatement(
                    "SELECT title FROM milestones WHERE team_id=? AND status='COMPLETED' ORDER BY due_date")) {
                milestones.setInt(1,currentTeam); try(ResultSet rs=milestones.executeQuery()) {
                    while(rs.next()) completed.append("<li>").append(htmlEscape(rs.getString(1))).append("</li>");
                }
            }
            String portfolioUrl=safeExternalUrl(link);
            String html="<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width'>"+
                    "<title>"+htmlEscape(name)+" | SkillForge Portfolio</title><style>body{margin:0;background:#0b1220;color:#eff5fa;font:16px Arial,sans-serif}"+
                    ".card{max-width:760px;margin:8vh auto;padding:42px;background:#1a2b3e;border:1px solid #2b3f53;border-radius:20px}"+
                    "h1{margin-bottom:6px}h2{color:#17b890;font-size:14px;text-transform:uppercase;letter-spacing:2px}p,li{color:#b8c8d6;line-height:1.7}"+
                    "a{color:#62eec7}</style></head><body><main class='card'><h2>SkillForge Portfolio</h2><h1>"+htmlEscape(name)+"</h1>"+
                    "<p>"+htmlEscape(level)+" · "+htmlEscape(primary)+" / "+htmlEscape(secondary)+"</p>"+
                    (team==null?"":"<p>Squad: "+htmlEscape(team)+"</p>")+
                    (portfolioUrl.isBlank()?"":"<p><a href='"+portfolioUrl+"'>Portfolio / project link</a></p>")+"<h2>Completed Milestones</h2><ul>"+
                    (completed.length()==0?"<li>Projects in progress</li>":completed.toString())+"</ul></main></body></html>";
            JFileChooser chooser=new JFileChooser(); chooser.setDialogTitle("Export Shareable Portfolio");
            chooser.setSelectedFile(new File("skillforge-portfolio.html"));
            if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
            File output=chooser.getSelectedFile();
            if(!output.getName().toLowerCase().endsWith(".html")) output=new File(output.getParentFile(),output.getName()+".html");
            if(output.exists() && JOptionPane.showConfirmDialog(this,"Replace the existing portfolio file?",
                    "Confirm Export",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            Files.writeString(output.toPath(),html,java.nio.charset.StandardCharsets.UTF_8);
            message("Portfolio exported to "+output.getAbsolutePath());
        } catch(Exception ex) { error(ex); }
    }

    private String htmlEscape(String value) {
        if(value==null) return "";
        return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private String safeExternalUrl(String value) {
        if(value==null||value.isBlank()) return "";
        try {
            java.net.URI uri=java.net.URI.create(value.trim());
            String scheme=uri.getScheme();
            return ("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme))?htmlEscape(value.trim()):"";
        } catch(IllegalArgumentException ex) { return ""; }
    }

    private void refreshAll() {
        loadProfile();
        loadSquads();
        loadMilestones();
        loadActivity();
        updateSquadActions(); updateMilestoneActions();
    }

    private void updateSquadActions() {
        if(joinAction==null) return;
        int row=squadTable.getSelectedRow();
        boolean selected=row>=0;
        boolean approved=selected && "APPROVED".equals(squadModel.getValueAt(squadTable.convertRowIndexToModel(row),6));
        joinAction.setEnabled(approved && currentTeam==null);
        leaveAction.setEnabled(currentTeam!=null);
        autoAction.setEnabled(currentTeam==null);
        if(membersMenuItem!=null) membersMenuItem.setEnabled(selected);
    }

    private void updateMilestoneActions() {
        if(submitAction==null) return;
        boolean selected=milestoneTable.getSelectedRow()>=0 && currentTeam!=null;
        submitAction.setEnabled(selected); resourceAction.setEnabled(selected);
    }

    private void loadProfile() {
        String sql = """
            SELECT p.participant_id, p.name, p.primary_role,
                   p.secondary_role, p.experience_level,
                   p.github_url, p.resume_file_name,
                   p.team_id, t.team_name, t.approval_status,
                   t.mentor_remarks, u.full_name AS mentor_name
            FROM participants p
            LEFT JOIN teams t ON p.team_id = t.team_id
            LEFT JOIN mentors m ON t.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            WHERE p.user_id = ?
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    profileLabel.setText("Student profile not found.");
                    return;
                }

                primaryRole = rs.getString("primary_role");
                experience = rs.getString("experience_level");
                currentTeam = (Integer) rs.getObject("team_id");

                String html = "<html><div style='width:600px'>" +
                        "<h2>MY PROFILE & STATUS</h2>" +
                        "<p>Registration ID: " + safe(rs.getString("participant_id")) + "</p>" +
                        "<p>Name: " + safe(rs.getString("name")) + "</p>" +
                        "<p>Primary Skill: " + safe(primaryRole) + "</p>" +
                        "<p>Secondary Skill: " + safe(rs.getString("secondary_role")) + "</p>" +
                        "<p>Experience: " + safe(experience) + "</p>" +
                        "<p>Portfolio / Project Link: " + safe(rs.getString("github_url")) + "</p>" +
                        "<p>Resume: " + safe(rs.getString("resume_file_name")) + "</p>" +
                        "<p>Current Squad: " + safe(rs.getString("team_name")) + "</p>" +
                        "<p>Mentor: " + safe(rs.getString("mentor_name")) + "</p>" +
                        "<p>Approval: " + safe(rs.getString("approval_status")) + "</p>" +
                        "<p>Remarks: " + safe(rs.getString("mentor_remarks")) + "</p>" +
                        "</div></html>";

                profileLabel.setText(html);
            }
        } catch (SQLException e) {
            error(e);
        }
    }

    private String safe(String s) {
        if (s == null) return "Not Assigned";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private double points(String tier) {
        if ("Advanced".equals(tier)) return 4.0;
        if ("Intermediate".equals(tier)) return 2.5;
        return 1.0;
    }

    private void loadSquads() {
        if (squadLoader != null && !squadLoader.isDone()) squadLoader.cancel(true);
        squadModel.setRowCount(0);
        squadIds.clear();

        String sql = """
            SELECT t.team_id, t.team_name, t.track, mentor.full_name AS mentor_name,
                   t.approval_status, t.max_frontend, t.max_backend,
                   t.max_ai, t.max_devops, p.primary_role,
                   p.experience_level
            FROM teams t
            LEFT JOIN participants p ON t.team_id = p.team_id
            LEFT JOIN mentors m ON t.mentor_id=m.mentor_id
            LEFT JOIN users mentor ON m.user_id=mentor.user_id
            ORDER BY t.team_id
            """;

        squadLoader = new SwingWorker<>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> rows = new ArrayList<>();
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    int lastId = -1, count = 0;
                    String name = "", track = "", mentor = "", status = "";
                    double score = 0;
                    int[] roleCounts = new int[4], roleQuotas = new int[4];
                    while (!isCancelled() && rs.next()) {
                        int id = rs.getInt("team_id");
                        if (lastId != -1 && id != lastId) {
                            rows.add(new Object[]{lastId, name, track, mentor, status, count, score, roleCounts, roleQuotas});
                            count = 0; score = 0; roleCounts = new int[4];
                        }
                        lastId = id; name = rs.getString("team_name"); track = rs.getString("track");
                        mentor = rs.getString("mentor_name"); status = rs.getString("approval_status");
                        roleQuotas = new int[]{rs.getInt("max_frontend"), rs.getInt("max_backend"), rs.getInt("max_ai"), rs.getInt("max_devops")};
                        String role = rs.getString("primary_role");
                        if (role != null) { count++; score += points(rs.getString("experience_level")); int index = roleIndex(role); if (index >= 0) roleCounts[index]++; }
                    }
                    if (!isCancelled() && lastId != -1)
                        rows.add(new Object[]{lastId, name, track, mentor, status, count, score, roleCounts, roleQuotas});
                }
                return rows;
            }

            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    for (Object[] row : get()) {
                        int id = (Integer) row[0];
                        addSquadRow(id, (String) row[1], (String) row[2], (String) row[3],
                                (String) row[4], (Integer) row[5], (Double) row[6], (int[]) row[7], (int[]) row[8]);
                    }
                    refreshSquadFilterChoices();
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    error(cause instanceof Exception exception ? exception : e);
                }
            }
        };
        squadLoader.execute();
    }

    private void addSquadRow(int id, String name, String track, String mentor,
                             String status, int count, double score,
                             int[] roleCounts, int[] roleQuotas) {
        String[] roleNames = {"Frontend", "Backend", "AI", "DevOps"};
        List<String> openings = new ArrayList<>();
        for (int i = 0; i < roleNames.length; i++) {
            int open = Math.max(0, roleQuotas[i] - roleCounts[i]);
            if (open > 0) openings.add(roleNames[i] + " ×" + open);
        }

        squadIds.add(id);
        squadModel.addRow(new Object[]{
                name, track, mentor == null ? "Unassigned" : mentor, count + " members",
                String.format("%.1f", score),
                openings.isEmpty() ? "Flexible membership" : String.join(", ", openings) + " · flexible",
                status
        });
    }

    private int roleIndex(String role) {
        return switch (role) {
            case "Frontend" -> 0;
            case "Backend" -> 1;
            case "AI" -> 2;
            case "DevOps" -> 3;
            default -> -1;
        };
    }

    private int selectedTeamId() {
        int row = squadTable.getSelectedRow();
        if (row < 0) return -1;
        return squadIds.get(squadTable.convertRowIndexToModel(row));
    }

    private void joinSelected() {
        int id = selectedTeamId();
        if (id == -1) {
            message("Select a squad first.");
            return;
        }
        joinTeam(id);
    }

    private void joinTeam(int teamId) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                // Lock the team first so concurrent joins serialize.
                int quota;
                String status;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT approval_status, max_frontend, " +
                                "max_backend, max_ai, max_devops " +
                                "FROM teams WHERE team_id=? FOR UPDATE")) {
                    ps.setInt(1, teamId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Squad not found.");

                        status = rs.getString("approval_status");

                        quota = switch (primaryRole) {
                            case "Frontend" -> rs.getInt("max_frontend");
                            case "Backend" -> rs.getInt("max_backend");
                            case "AI" -> rs.getInt("max_ai");
                            case "DevOps" -> rs.getInt("max_devops");
                            default -> 0;
                        };
                    }
                }

                if (!"APPROVED".equals(status))
                    throw new SQLException("You can join a squad after it has been approved.");

                // Verify that this student is still unassigned.
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT team_id FROM participants " +
                                "WHERE user_id=? FOR UPDATE")) {
                    ps.setInt(1, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Student not found.");
                        if (rs.getObject("team_id") != null)
                            throw new SQLException("Leave your current squad first.");
                    }
                }

                int count = 0, beginners = 0, roleCount = 0;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT primary_role, experience_level " +
                                "FROM participants WHERE team_id=?")) {
                    ps.setInt(1, teamId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            count++;
                            if ("Beginner".equals(rs.getString("experience_level")))
                                beginners++;
                            if (primaryRole.equals(rs.getString("primary_role")))
                                roleCount++;
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE participants SET team_id=? " +
                                "WHERE user_id=? AND team_id IS NULL")) {
                    ps.setInt(1, teamId);
                    ps.setInt(2, userId);

                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Squad assignment failed.");
                }

                ActivitySupport.record(c, teamId, userId, "SQUAD_JOINED", "Joined the squad.");

                c.commit();
                message("Successfully joined the squad!");

            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            error(e);
        }
        refreshAll();
    }

    private void leaveSquad() {
        if (currentTeam == null) {
            message("You are not in a squad.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this, "Leave your current squad?",
                "Confirm", JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE participants SET team_id=NULL WHERE user_id=?")) {
            ps.setInt(1, userId);
            if (ps.executeUpdate() == 1) ActivitySupport.record(c, currentTeam, userId,
                    "SQUAD_LEFT", "Left the squad.");
            message("You left the squad.");
        } catch (SQLException e) {
            error(e);
        }

        refreshAll();
    }

    private void autoAssign() {
        if (currentTeam != null) {
            message("Leave your current squad before auto-assignment.");
            return;
        }

        String sql = """
            SELECT t.team_id, t.team_name, t.track,
                   t.max_frontend, t.max_backend, t.max_ai, t.max_devops,
                   p.primary_role, p.experience_level
            FROM teams t
            LEFT JOIN participants p ON t.team_id=p.team_id
            WHERE t.approval_status = 'APPROVED'
            ORDER BY t.team_id
            """;

        int bestId = -1;
        double bestScore = -1;
        String bestName = "", bestTrack = "";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int id = -1, count = 0, beginners = 0, roleCount = 0;
            int quota = 0;
            String teamName = "", track = "";
            List<String> roles = new ArrayList<>();

            while (rs.next()) {
                int nextId = rs.getInt("team_id");

                if (id != -1 && id != nextId) {
                    double s = matchScore(count, beginners, roleCount,
                            quota, roles);
                    if (s > bestScore || (s >= 0 && s == bestScore &&
                            java.util.concurrent.ThreadLocalRandom.current().nextBoolean())) {
                        bestScore = s;
                        bestId = id;
                        bestName = teamName;
                        bestTrack = track;
                    }
                    count = beginners = roleCount = 0;
                    roles.clear();
                }

                id = nextId;
                teamName = rs.getString("team_name");
                track = rs.getString("track");
                quota = switch (primaryRole) {
                    case "Frontend" -> rs.getInt("max_frontend");
                    case "Backend" -> rs.getInt("max_backend");
                    case "AI" -> rs.getInt("max_ai");
                    case "DevOps" -> rs.getInt("max_devops");
                    default -> 0;
                };

                String role = rs.getString("primary_role");
                if (role != null) {
                    count++;
                    roles.add(role);
                    if (role.equals(primaryRole)) roleCount++;
                    if ("Beginner".equals(rs.getString("experience_level")))
                        beginners++;
                }
            }

            if (id != -1) {
                double s = matchScore(count, beginners, roleCount,
                        quota, roles);
                if (s > bestScore || (s >= 0 && s == bestScore &&
                        java.util.concurrent.ThreadLocalRandom.current().nextBoolean())) {
                    bestScore = s;
                    bestId = id;
                    bestName = teamName;
                    bestTrack = track;
                }
            }

        } catch (SQLException e) {
            error(e);
            return;
        }

        if (bestId == -1 || bestScore < 0) {
            message("No eligible squad found. Create a new squad.");
        } else {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Recommended squad: " + bestName + " (" + bestTrack + ")" +
                            "\nYour primary skill has an open role." +
                            "\nMatch score: " + String.format("%.1f", bestScore) +
                            "\nJoin this squad?",
                    "Smart Match",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION)
                joinTeam(bestId);
        }
    }

    private double matchScore(int count, int beginners,
                              int roleCount, int quota,
                              List<String> roles) {
        double score = 0;
        if (!roles.contains(primaryRole)) score += 40;
        score += 25;
        score += 25.0 / (count + 1);
        if ("Advanced".equals(experience) && beginners > 0)
            score += 15;
        else if ("Intermediate".equals(experience))
            score += 10;

        return score;
    }

    private void createSquad() {
        if (currentTeam != null) {
            message("Leave your current squad first.");
            return;
        }

        JTextField name = new JTextField();
        JTextField track = new JTextField();

        JSpinner frontend = spinner(1);
        JSpinner backend = spinner(1);
        JSpinner ai = spinner(1);
        JSpinner devops = spinner(1);

        JComboBox<MentorOption> mentors = new JComboBox<>();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT m.mentor_id, u.full_name
                 FROM mentors m
                 JOIN users u ON m.user_id=u.user_id
                 WHERE (
                     SELECT COUNT(*) FROM teams t
                     WHERE t.mentor_id=m.mentor_id
                       AND t.approval_status IN ('PENDING','APPROVED')
                 ) < m.max_squads
                 ORDER BY u.full_name
                 """);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                mentors.addItem(new MentorOption(
                        rs.getInt("mentor_id"),
                        rs.getString("full_name")
                ));
            }
        } catch (SQLException e) {
            error(e);
            return;
        }

        if (mentors.getItemCount() == 0) {
            message("No mentors currently have capacity.");
            return;
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        addField(form, g, 0, "Squad Name", name);
        addField(form, g, 1, "Hackathon Track", track);
        addField(form, g, 2, "Suggested Frontend Mix", frontend);
        addField(form, g, 3, "Suggested Backend Mix", backend);
        addField(form, g, 4, "Suggested AI Mix", ai);
        addField(form, g, 5, "Suggested DevOps Mix", devops);
        addField(form, g, 6, "Mentor", mentors);
        JLabel policyHint=new JLabel("Squad membership is unlimited; this is only a suggested skill mix.");
        policyHint.setForeground(AppTheme.MUTED);
        addField(form,g,7,"Note",policyHint);

        int choice = AppTheme.showFormDialog(this, form, "Create New Squad");

        if (choice != JOptionPane.OK_OPTION) return;

        int f = (int) frontend.getValue();
        int b = (int) backend.getValue();
        int a = (int) ai.getValue();
        int d = (int) devops.getValue();

        if (name.getText().isBlank() || track.getText().isBlank()) {
            message("Enter squad name and track.");
            return;
        }

        if (f + b + a + d != 4) {
            message("Set the four suggested role openings to a total of 4, then try again.");
            return;
        }

        MentorOption mentor = (MentorOption) mentors.getSelectedItem();
        if (mentor == null) return;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                int max;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT max_squads FROM mentors " +
                                "WHERE mentor_id=? FOR UPDATE")) {
                    ps.setInt(1, mentor.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Mentor not found.");
                        max = rs.getInt(1);
                    }
                }

                int used;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM teams WHERE mentor_id=? " +
                                "AND approval_status IN ('PENDING','APPROVED')")) {
                    ps.setInt(1, mentor.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        used = rs.getInt(1);
                    }
                }

                if (used >= max)
                    throw new SQLException("Mentor capacity reached.");

                int teamId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO teams " +
                                "(team_name,track,mentor_id,max_frontend," +
                                "max_backend,max_ai,max_devops,approval_status) " +
                                "VALUES(?,?,?,?,?,?,?,'PENDING')",
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, name.getText().trim());
                    ps.setString(2, track.getText().trim());
                    ps.setInt(3, mentor.id);
                    ps.setInt(4, f);
                    ps.setInt(5, b);
                    ps.setInt(6, a);
                    ps.setInt(7, d);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next())
                            throw new SQLException("Team creation failed.");
                        teamId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE participants SET team_id=? " +
                                "WHERE user_id=? AND team_id IS NULL")) {
                    ps.setInt(1, teamId);
                    ps.setInt(2, userId);

                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Student already assigned.");
                }

                ActivitySupport.record(c, teamId, userId, "SQUAD_CREATED", "Created a squad and joined its roster.");

                c.commit();
                message("Squad created! Awaiting mentor approval.");

            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            error(e);
        }

        refreshAll();
    }

    private JSpinner spinner(int value) {
        return new JSpinner(new SpinnerNumberModel(value, 0, 4, 1));
    }

    private void addField(JPanel p, GridBagConstraints g,
                          int row, String label, JComponent input) {
        g.gridy = row;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        p.add(input, g);
    }

    private static class MentorOption {
        int id;
        String name;

        MentorOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private void viewMembers() {
        int teamId = selectedTeamId();
        if (teamId == -1) {
            message("Select a squad first.");
            return;
        }

        String sql = """
            SELECT name, primary_role, secondary_role, experience_level
            FROM participants
            WHERE team_id=?
            ORDER BY name
            """;

        StringBuilder text = new StringBuilder();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    text.append(rs.getString("name"))
                            .append(" | ")
                            .append(rs.getString("primary_role"))
                            .append(" | ")
                            .append(rs.getString("experience_level"))
                            .append("\n");
                }
            }
        } catch (SQLException e) {
            error(e);
            return;
        }

        message(text.length() == 0 ?
                "No members yet." : text.toString());
    }

    private void loadMilestones() {
        milestoneModel.setRowCount(0);
        milestoneIds.clear();

        if (currentTeam == null) {
            progressLabel.setText("Join a squad to see milestones and progress.");
            progressBar.setValue(0); progressBar.setString("No squad");
            overdueLabel.setText("0 overdue");
            return;
        }

        String sql = """
            SELECT m.milestone_id, m.title, m.due_date, m.status,
                   m.attachment_name, m.resource_link,
                   s.submitted_at, s.review_status, s.review_feedback
            FROM milestones m
            LEFT JOIN milestone_submissions s
              ON s.milestone_id=m.milestone_id AND s.participant_id=(
                SELECT participant_id FROM participants WHERE user_id=?
              )
            WHERE m.team_id=? ORDER BY m.due_date
            """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, currentTeam);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date due = rs.getDate("due_date");
                    String reminder = due != null && !"COMPLETED".equals(rs.getString("status"))
                            ? (due.toLocalDate().isBefore(java.time.LocalDate.now()) ? "OVERDUE" :
                            due.toLocalDate().isBefore(java.time.LocalDate.now().plusDays(3)) ? "Due soon" : "") : "";
                    milestoneIds.add(rs.getInt("milestone_id"));
                    milestoneModel.addRow(new Object[]{rs.getString("title"), due,
                            rs.getString("status"), reminder,
                            rs.getTimestamp("submitted_at") == null ? "Not submitted" : "Submitted",
                            rs.getString("review_status") == null ? "Not submitted" : rs.getString("review_status"),
                            rs.getString("review_feedback") == null ? "" : rs.getString("review_feedback")});
                }
            }
            try (PreparedStatement progressQuery = c.prepareStatement(
                    "SELECT COUNT(*), SUM(status='COMPLETED') FROM milestones WHERE team_id=?")) {
                progressQuery.setInt(1, currentTeam);
                try (ResultSet rs = progressQuery.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt(1), completed = rs.getInt(2);
                        progressLabel.setText("Milestones");
                        progressBar.setValue(total == 0 ? 0 : completed * 100 / total);
                        progressBar.setString(total == 0 ? "No milestones yet" : (completed * 100 / total) + "% complete");
                    }
                }
            }
            try (PreparedStatement overdueQuery=c.prepareStatement(
                    "SELECT COUNT(*) FROM milestones WHERE team_id=? AND due_date<CURDATE() AND status<>'COMPLETED'")) {
                overdueQuery.setInt(1,currentTeam); try(ResultSet rs=overdueQuery.executeQuery()) {
                    if(rs.next()) overdueLabel.setText(rs.getInt(1)+" overdue");
                }
            }
        } catch (SQLException e) {
            error(e);
        }
    }

    private int selectedMilestoneId() {
        int row = milestoneTable.getSelectedRow();
        return row < 0 ? -1 : milestoneIds.get(milestoneTable.convertRowIndexToModel(row));
    }

    private void submitMilestone() {
        int id = selectedMilestoneId();
        if (id < 0) { message("Select a milestone first."); return; }
        JTextField link = new JTextField();
        JLabel fileLabel = new JLabel("No file selected");
        final File[] file = {null};
        JButton choose = button("CHOOSE FILE", blue);
        choose.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File picked = chooser.getSelectedFile();
                if (picked.length() > 10L * 1024 * 1024) { message("Maximum file size is 10 MB."); return; }
                file[0] = picked; fileLabel.setText(picked.getName());
            }
        });
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Submission link")); form.add(link);
        form.add(choose); form.add(fileLabel);
        JLabel hint=new JLabel("Files up to 10 MB. You may provide a link, a file, or both.");
        hint.setForeground(AppTheme.MUTED); form.add(hint); form.add(new JLabel(""));
        if (AppTheme.showFormDialog(this, AppTheme.formSection("YOUR SUBMISSION",form), "Submit Milestone") != JOptionPane.OK_OPTION) return;
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO milestone_submissions(milestone_id,participant_id,submission_link,file_name,file_data) " +
                "SELECT ?,participant_id,?,?,? FROM participants WHERE user_id=? AND team_id=? " +
            "ON DUPLICATE KEY UPDATE submission_link=VALUES(submission_link),file_name=VALUES(file_name),file_data=VALUES(file_data),submitted_at=CURRENT_TIMESTAMP,review_status='PENDING',review_feedback=NULL")) {
            ps.setInt(1, id); ps.setString(2, link.getText().trim().isEmpty() ? null : link.getText().trim());
            ps.setString(3, file[0] == null ? null : file[0].getName());
            if (file[0] == null) ps.setNull(4, Types.BLOB); else ps.setBytes(4, Files.readAllBytes(file[0].toPath()));
            ps.setInt(5, userId); ps.setInt(6, currentTeam);
            if (ps.executeUpdate() < 1) throw new SQLException("Submission was not saved.");
            ActivitySupport.record(c, currentTeam, userId, "SUBMISSION", "Submitted or resubmitted milestone work.");
            message("Submission saved.");
        } catch (Exception ex) { error(ex); }
        refreshAll();
    }

    private void openMilestoneResource() {
        int id = selectedMilestoneId();
        if (id < 0) { message("Select a milestone first."); return; }
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT attachment_name,attachment,resource_link FROM milestones WHERE milestone_id=? AND team_id=?")) {
            ps.setInt(1, id); ps.setInt(2, currentTeam);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                String link = rs.getString("resource_link");
                if (link != null && !link.isBlank()) { Desktop.getDesktop().browse(java.net.URI.create(link)); return; }
                byte[] bytes = rs.getBytes("attachment");
                if (bytes == null) { message("No resource attached."); return; }
                File temp = File.createTempFile("skillforge-resource-", "-" + rs.getString("attachment_name"));
                Files.write(temp.toPath(), bytes); Desktop.getDesktop().open(temp);
            }
        } catch (Exception ex) { error(ex); }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(
                this, e.getMessage(),
                "SkillForge Error", JOptionPane.ERROR_MESSAGE
        );
    }

    private int quotaTotal(Connection c, int teamId) throws SQLException {
        String sql = """
            SELECT max_frontend + max_backend + max_ai + max_devops
            FROM teams
            WHERE team_id=?
            """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Squad not found.");
                }
                return rs.getInt(1);
            }
        }
    }
}


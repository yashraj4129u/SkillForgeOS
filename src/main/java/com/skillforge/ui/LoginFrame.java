
package com.skillforge.ui;

import com.skillforge.db.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File; 
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;

    private static final Color BG = AppTheme.BACKGROUND;
    private static final Color PANEL = AppTheme.CARD;
    private static final Color GREEN = AppTheme.GREEN;
    private static final Color BLUE = AppTheme.BLUE;
    private static final Color WHITE = AppTheme.TEXT;
    private static final Color TEXT = AppTheme.TEXT;

    public LoginFrame() {
        setTitle("SkillForge OS - Authentication");
        setSize(940, 680);
        setMinimumSize(new Dimension(780, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        BrandPanel welcome = new BrandPanel();
        welcome.setPreferredSize(new Dimension(360, 680));
        welcome.setBorder(new EmptyBorder(44, 38, 40, 34));
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("SKILLFORGE  /  OS");
        brand.setForeground(new Color(98, 238, 199));
        brand.setFont(new Font("SansSerif", Font.BOLD, 13));
        welcome.add(brand);
        welcome.add(Box.createVerticalStrut(94));
        JLabel welcomeTitle = new JLabel("<html>Build your next<br>big idea.</html>");
        welcomeTitle.setForeground(Color.WHITE);
        welcomeTitle.setFont(new Font("SansSerif", Font.BOLD, 34));
        welcomeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcome.add(welcomeTitle);
        welcome.add(Box.createVerticalStrut(18));
        JLabel welcomeCopy = new JLabel("<html><div style='width:255px;line-height:1.5'>Find your team, learn from mentors, and turn hackathon ideas into real projects.</div></html>");
        welcomeCopy.setForeground(new Color(205, 220, 232));
        welcomeCopy.setFont(new Font("SansSerif", Font.PLAIN, 15));
        welcomeCopy.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcome.add(welcomeCopy);
        welcome.add(Box.createVerticalGlue());
        welcome.add(featureLine("01", "Discover your community"));
        welcome.add(Box.createVerticalStrut(16));
        welcome.add(featureLine("02", "Make progress together"));
        welcome.add(Box.createVerticalStrut(16));
        welcome.add(featureLine("03", "Show what you can do"));
        welcome.add(Box.createVerticalStrut(8));

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(BG);
        right.setBorder(new EmptyBorder(34, 38, 34, 38));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(34, 38, 32, 38)));

        JLabel eyebrow = new JLabel("YOUR WORKSPACE");
        eyebrow.setForeground(GREEN);
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(eyebrow);
        card.add(Box.createVerticalStrut(8));
        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(7));
        JLabel subtitle = new JLabel("Sign in to continue to your dashboard.");
        subtitle.setForeground(AppTheme.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));

        card.add(makeLabel("USERNAME"));
        usernameField = new JTextField(22);
        styleField(usernameField);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(17));

        card.add(makeLabel("PASSWORD"));

        passwordField = new JPasswordField(22);
        styleField(passwordField);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(17));

        card.add(makeLabel("SIGN IN AS"));

        roleBox = new JComboBox<>(
                new String[]{"STUDENT", "MENTOR", "ADMIN"}
        );
        roleBox.setBackground(AppTheme.SURFACE);
        roleBox.setForeground(TEXT);
        roleBox.setFont(new Font("SansSerif", Font.PLAIN, 15));
        AppTheme.styleComboBox(roleBox);
        roleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleBox.setBorder(new LineBorder(AppTheme.BORDER, 1, true));
        card.add(roleBox);
        card.add(Box.createVerticalStrut(26));

        JButton loginButton = new JButton("LOGIN TO DASHBOARD");
        styleButton(loginButton, GREEN);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(loginButton);
        card.add(Box.createVerticalStrut(12));

        JButton registerButton = new JButton("REGISTER NEW ACCOUNT");
        styleButton(registerButton, BLUE);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(registerButton);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());
        getRootPane().setDefaultButton(loginButton);

        right.add(card);
        root.add(welcome, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JLabel featureLine(String number, String text) {
        JLabel label = new JLabel("<html><span style='color:#62EEC7;font-weight:bold'>" + number +
                "</span>&nbsp;&nbsp;&nbsp;<span style='color:#E9F1F7'>" + text + "</span></html>");
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static final class BrandPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(20, 94, 91),
                    getWidth(), getHeight(), new Color(22, 48, 73)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 14));
            g.fillOval(getWidth() - 135, -95, 250, 250);
            g.fillOval(getWidth() - 80, getHeight() - 200, 190, 190);
            g.dispose();
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 1, 7, 0));
        return label;
    }

    private void styleField(JTextField field) {
        field.setBackground(AppTheme.SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(GREEN);
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(300, 46));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
    }

    private void styleButton(JButton button, Color color) {
        AppTheme.styleButton(button, color);
        button.setPreferredSize(new Dimension(300, 46));
    }

    private void login() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Please enter username and password."
            );
            return;
        }

        String sql = "SELECT user_id, role FROM users " +
                "WHERE username=? AND password=? AND role=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    int userId = rs.getInt("user_id");
                    String userRole = rs.getString("role");

                    openDashboard(userId, userRole);

                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Invalid username, password or role.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void openDashboard(int userId, String role) {

        JFrame dashboard;

        switch (role) {
            case "STUDENT":
                dashboard = new StudentDashboard(userId);
                break;

            case "MENTOR":
                dashboard = new MentorDashboard(userId);
                break;

            case "ADMIN":

                dashboard = new AdminDashboard(userId);

                break;

            default:
                JOptionPane.showMessageDialog(
                        this, "Unknown role."
                );
                return;
        }

        dashboard.setVisible(true);
        dispose();
    }

    private void register() {

        JTextField fullName = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        JComboBox<String> role = new JComboBox<>(
                new String[]{"STUDENT", "MENTOR"}
        );

        JTextField registrationId = new JTextField();

        JComboBox<String> primaryRole = new JComboBox<>(
                new String[]{"Frontend", "Backend", "AI", "DevOps"}
        );

        JComboBox<String> secondaryRole = new JComboBox<>(
                new String[]{"Frontend", "Backend", "AI", "DevOps"}
        );

        JComboBox<String> experience = new JComboBox<>(
                new String[]{"Beginner", "Intermediate", "Advanced"}
        );

        JTextField specialization = new JTextField();
        JTextField github = new JTextField();
        JLabel resumeName = new JLabel("No resume selected");
        File[] resume = new File[1];
        JButton chooseResume = new JButton("CHOOSE FILE");
        AppTheme.styleButton(chooseResume, BLUE);
        chooseResume.addActionListener(e -> {
            File chosen = PortfolioSupport.chooseResume(this, resumeName);
            if (chosen != null) resume[0] = chosen;
        });
        JPanel resumeRow = new JPanel(new BorderLayout(8, 0));
        resumeRow.setBackground(AppTheme.SURFACE);
        resumeRow.add(chooseResume, BorderLayout.WEST);
        resumeRow.add(resumeName, BorderLayout.CENTER);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JPanel accountFields = new JPanel(new GridLayout(0, 2, 10, 10));
        accountFields.add(new JLabel("Full Name"));
        accountFields.add(fullName);
        accountFields.add(new JLabel("Username"));
        accountFields.add(username);
        accountFields.add(new JLabel("Password"));
        accountFields.add(password);
        accountFields.add(new JLabel("Account Type"));
        accountFields.add(role);

        JPanel roleFields = new JPanel(new GridLayout(0, 2, 10, 10));
        JLabel githubLabel = new JLabel("Portfolio / Project Link");
        JLabel resumeLabel = new JLabel("Resume (optional)");
        JLabel specializationLabel = new JLabel("Mentor Specialization");
        Runnable updateRoleFields = () -> {
            roleFields.removeAll();
            boolean student = "STUDENT".equals(role.getSelectedItem());
            if (student) {
                roleFields.add(new JLabel("Student Registration ID")); roleFields.add(registrationId);
                roleFields.add(new JLabel("Primary Skill")); roleFields.add(primaryRole);
                roleFields.add(new JLabel("Secondary Skill")); roleFields.add(secondaryRole);
                roleFields.add(new JLabel("Experience Level")); roleFields.add(experience);
                roleFields.add(githubLabel); roleFields.add(github);
                roleFields.add(resumeLabel); roleFields.add(resumeRow);
            } else {
                roleFields.add(specializationLabel); roleFields.add(specialization);
            }
            AppTheme.styleForm(roleFields);
            roleFields.revalidate();
            roleFields.repaint();
            form.revalidate();
            form.repaint();
            AppTheme.refreshFormDialog(form);
        };
        role.addActionListener(e -> updateRoleFields.run());
        form.add(AppTheme.formSection("ACCOUNT",accountFields));
        form.add(Box.createVerticalStrut(12));
        form.add(AppTheme.formSection("ROLE DETAILS",roleFields));
        updateRoleFields.run();

        int choice = AppTheme.showFormDialog(this, form, "Register New Account");

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String name = fullName.getText().trim();
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String selectedRole = (String) role.getSelectedItem();

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Name, username and password are required."
            );
            return;
        }

        if (selectedRole.equals("STUDENT") &&
                registrationId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Student Registration ID is required."
            );
            return;
        }

        if (selectedRole.equals("MENTOR") &&
                specialization.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Mentor specialization is required."
            );
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int newUserId;

                String userSql =
                        "INSERT INTO users " +
                                "(username,password,role,full_name) " +
                                "VALUES(?,?,?,?)";

                try (PreparedStatement ps = con.prepareStatement(
                        userSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, user);
                    ps.setString(2, pass);
                    ps.setString(3, selectedRole);
                    ps.setString(4, name);

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Failed to generate user ID."
                            );
                        }

                        newUserId = keys.getInt(1);
                    }
                }

                if (selectedRole.equals("STUDENT")) {

                    String studentSql =
                            "INSERT INTO participants " +
                                    "(participant_id,user_id,name,primary_role," +
                                    "secondary_role,experience_level,github_url," +
                                    "resume_file_name,resume_file) " +
                                    "VALUES(?,?,?,?,?,?,?,?,?)";

                    try (PreparedStatement ps =
                                 con.prepareStatement(studentSql)) {

                        ps.setString(
                                1, registrationId.getText().trim()
                        );
                        ps.setInt(2, newUserId);
                        ps.setString(3, name);
                        ps.setString(
                                4, (String) primaryRole.getSelectedItem()
                        );
                        ps.setString(
                                5, (String) secondaryRole.getSelectedItem()
                        );
                        ps.setString(
                                6, (String) experience.getSelectedItem()
                        );
                        ps.setString(7, github.getText().trim().isEmpty() ? null : github.getText().trim());
                        PortfolioSupport.bindResume(ps, 8, 9, resume[0]);

                        ps.executeUpdate();
                    }

                } else {

                    String mentorSql =
                            "INSERT INTO mentors " +
                                    "(user_id,specialization,max_squads) " +
                                    "VALUES(?,?,3)";

                    try (PreparedStatement ps =
                                 con.prepareStatement(mentorSql)) {

                        ps.setInt(1, newUserId);
                        ps.setString(
                                2, specialization.getText().trim()
                        );

                        ps.executeUpdate();
                    }
                }

                con.commit();

                JOptionPane.showMessageDialog(
                        this, "Account registered successfully!"
                );

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            AppTheme.install();
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}


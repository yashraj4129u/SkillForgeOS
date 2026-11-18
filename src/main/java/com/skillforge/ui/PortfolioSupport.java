package com.skillforge.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/** Helpers shared by student create/edit forms for portfolio details. */
public final class PortfolioSupport {
    public static final long MAX_RESUME_BYTES = 10L * 1024 * 1024;

    private PortfolioSupport() { }

    public static File chooseResume(Component parent, JLabel fileLabel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose student resume");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Resume files (PDF, DOC, DOCX)", "pdf", "doc", "docx"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;

        File file = chooser.getSelectedFile();
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"))) {
            JOptionPane.showMessageDialog(parent, "Choose a PDF, DOC, or DOCX resume.");
            return null;
        }
        if (!file.isFile() || file.length() > MAX_RESUME_BYTES) {
            JOptionPane.showMessageDialog(parent, "The resume must be a file smaller than 10 MB.");
            return null;
        }
        fileLabel.setText(file.getName());
        return file;
    }

    public static void bindResume(PreparedStatement ps, int fileNameIndex,
                                  int blobIndex, File file) throws SQLException {
        if (file == null) {
            ps.setNull(fileNameIndex, Types.VARCHAR);
            ps.setNull(blobIndex, Types.BLOB);
        } else {
            ps.setString(fileNameIndex, file.getName());
            try {
                if (file.length() > MAX_RESUME_BYTES) throw new SQLException("Resume file is larger than 10 MB.");
                ps.setBytes(blobIndex, Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                throw new SQLException("Could not read the selected resume file.", e);
            }
        }
    }
}

package com.skillforge.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;

/** Shared visual system for the SkillForgeOS desktop application. */
public final class AppTheme {
    public static final Color BACKGROUND = new Color(11, 18, 32);
    public static final Color SURFACE = new Color(18, 31, 48);
    public static final Color CARD = new Color(26, 43, 62);
    public static final Color BORDER = new Color(43, 63, 83);
    public static final Color TEXT = new Color(239, 245, 250);
    public static final Color MUTED = new Color(157, 176, 194);
    public static final Color GREEN = new Color(23, 184, 144);
    public static final Color BLUE = new Color(72, 136, 245);
    public static final Color RED = new Color(226, 101, 111);
    public static final Color AMBER = new Color(231, 173, 88);

    private static final Font BODY = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON = new Font("SansSerif", Font.BOLD, 13);

    private AppTheme() { }

    public static void install() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Swing's current look and feel remains a usable fallback.
        }

        UIManager.put("defaultFont", BODY);
        UIManager.put("control", BACKGROUND);
        UIManager.put("text", TEXT);
        UIManager.put("nimbusBase", CARD);
        UIManager.put("nimbusBlueGrey", BORDER);
        UIManager.put("nimbusLightBackground", SURFACE);
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", MUTED);
        UIManager.put("TabbedPane.selected", CARD);
        UIManager.put("TabbedPane.contentAreaColor", BACKGROUND);
        UIManager.put("TabbedPane.tabAreaBackground", BACKGROUND);
        UIManager.put("TabbedPane.focus", BACKGROUND);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(8, 2, 2, 2));
        UIManager.put("TabbedPane.tabInsets", new Insets(10, 18, 10, 18));
        UIManager.put("TabbedPane.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", new Color(39, 68, 103));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.background", CARD);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.buttonBackground", SURFACE);
        UIManager.put("ComboBox.buttonArrowColor", MUTED);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageArea.background", SURFACE);
        UIManager.put("OptionPane.buttonArea.background", SURFACE);
        UIManager.put("OptionPane.foreground", TEXT);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BUTTON);
        UIManager.put("OptionPane.border", BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true), new EmptyBorder(10, 12, 10, 12)));
        UIManager.put("OptionPane.messageAreaBorder", new EmptyBorder(4, 4, 4, 4));
        UIManager.put("OptionPane.buttonAreaBorder", new EmptyBorder(8, 4, 2, 4));
        UIManager.put("OptionPane.separatorColor", BORDER);
        UIManager.put("ScrollPane.background", BACKGROUND);
        UIManager.put("ToolTip.background", CARD);
        UIManager.put("ToolTip.foreground", TEXT);
    }

    public static JPanel header(String section, String subtitle, JButton action) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(2, 2, 18, 2));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("SKILLFORGE OS");
        brand.setForeground(GREEN);
        brand.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel heading = new JLabel(section);
        heading.setForeground(TEXT);
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel detail = new JLabel(subtitle);
        detail.setForeground(MUTED);
        detail.setFont(new Font("SansSerif", Font.PLAIN, 13));

        titles.add(brand);
        titles.add(Box.createVerticalStrut(3));
        titles.add(heading);
        titles.add(Box.createVerticalStrut(2));
        titles.add(detail);

        if (action != null) {
            styleButton(action, RED);
            header.add(action, BorderLayout.EAST);
        }
        header.add(titles, BorderLayout.CENTER);
        return header;
    }

    /** Applies the same compact, high contrast tab style to every dashboard. */
    public static void styleTabs(JTabbedPane tabs) {
        tabs.setOpaque(false);
        tabs.setBackground(BACKGROUND);
        tabs.setForeground(MUTED);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setFocusable(false);
        tabs.setBorder(new EmptyBorder(0, 0, 0, 0));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(12, 17, 12, 17);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(12, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics graphics, int placement,
                    int tabIndex, int x, int y, int width, int height, boolean selected) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(selected ? SURFACE : new Color(18, 31, 48, 150));
                g.fillRoundRect(x + 2, y + 2, width - 4, height + 3, 12, 12);
                if (selected) {
                    g.setColor(GREEN);
                    g.fillRoundRect(x + 14, y + height - 3, width - 28, 3, 3, 3);
                }
                g.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics graphics, int placement,
                    int tabIndex, int x, int y, int width, int height, boolean selected) { }

            @Override
            protected void paintContentBorder(Graphics graphics, int placement, int selectedIndex) { }

            @Override
            protected void paintText(Graphics graphics, int placement, Font font,
                    FontMetrics metrics, int tabIndex, String title,
                    Rectangle textRect, boolean selected) {
                graphics.setFont(font);
                graphics.setColor(selected ? TEXT : MUTED);
                graphics.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            }
        });
    }

    public static void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setUI(new SoftButtonUI(color));
        button.setToolTipText(button.getText());
    }

    private static final class SoftButtonUI extends BasicButtonUI {
        private final Color color;

        private SoftButtonUI(Color color) { this.color = color; }

        @Override
        public void paint(Graphics graphics, JComponent component) {
            JButton button = (JButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = color;
            if (!button.isEnabled()) fill = new Color(69, 82, 98);
            else if (button.getModel().isPressed()) fill = color.darker();
            else if (button.getModel().isRollover()) fill = color.brighter();
            g.setColor(fill);
            g.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 15, 15);
            g.dispose();
            super.paint(graphics, component);
        }
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setSelectionBackground(new Color(39, 68, 103));
        table.setSelectionForeground(TEXT);
        table.setGridColor(BACKGROUND);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(CARD);
        header.setForeground(MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected,
                        focused, row, column);
                setForeground(selected ? TEXT : AppTheme.TEXT);
                setBackground(selected ? new Color(35, 75, 91)
                        : (row % 2 == 0 ? SURFACE : new Color(21, 36, 54)));
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(BODY);
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Number.class, renderer);
    }

    public static JTable emptyTable(DefaultTableModel model, String emptyMessage) {
        return new JTable(model) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (getRowCount() != 0) return;
                Graphics2D g=(Graphics2D)graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(MUTED); g.setFont(new Font("SansSerif",Font.PLAIN,15));
                FontMetrics metrics=g.getFontMetrics();
                int x=Math.max(12,(getWidth()-metrics.stringWidth(emptyMessage))/2);
                int y=Math.max(metrics.getAscent()+18,getHeight()/2);
                g.drawString(emptyMessage,x,y); g.dispose();
            }
        };
    }

    public static void setColumnWidths(JTable table, int... widths) {
        for(int i=0;i<Math.min(widths.length,table.getColumnModel().getColumnCount());i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(Math.min(70,widths[i]));
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    public static void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(SURFACE); combo.setForeground(TEXT); combo.setFont(BODY);
        combo.setOpaque(true);
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow=new JButton("▾");
                arrow.setForeground(MUTED); arrow.setBackground(SURFACE);
                arrow.setFont(new Font("SansSerif",Font.BOLD,14));
                arrow.setFocusPainted(false); arrow.setBorder(new EmptyBorder(0,8,0,8));
                arrow.setOpaque(true); arrow.setContentAreaFilled(true);
                return arrow;
            }
        });
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,
                    boolean selected,boolean focused) {
                super.getListCellRendererComponent(list,value,index,selected,focused);
                setBackground(selected?new Color(39,68,103):SURFACE);
                setForeground(TEXT); setBorder(new EmptyBorder(7,9,7,9));
                return this;
            }
        });
        combo.setBorder(new LineBorder(BORDER,1,true));
    }

    public static JScrollPane styleScrollPane(JTable table) {
        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(SURFACE);
        scroll.setBackground(SURFACE);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        return scroll;
    }

    /** Renders compact, color-coded status and risk cells. */
    public static void styleStatusColumn(JTable table, int column) {
        if (column < 0 || column >= table.getColumnModel().getColumnCount()) return;
        table.getColumnModel().getColumn(column).setCellRenderer(new StatusBadgeRenderer());
    }

    public static void stylePopupMenu(JPopupMenu menu) {
        menu.setBackground(SURFACE);
        menu.setBorder(new LineBorder(BORDER,1,true));
        for(Component component:menu.getComponents()) if(component instanceof JMenuItem item) {
            item.setBackground(SURFACE); item.setForeground(TEXT);
            item.setFont(new Font("SansSerif",Font.PLAIN,13));
            item.setBorder(new EmptyBorder(8,14,8,14));
            item.setOpaque(true);
        }
    }

    public static JPanel formSection(String title, Component contents) {
        JPanel section=new JPanel(new BorderLayout(8,8)); section.setBackground(SURFACE);
        TitledBorder border=BorderFactory.createTitledBorder(new LineBorder(BORDER,1,true),title);
        border.setTitleColor(GREEN); border.setTitleFont(new Font("SansSerif",Font.BOLD,11));
        section.setBorder(BorderFactory.createCompoundBorder(border,new EmptyBorder(8,10,10,10)));
        section.add(contents,BorderLayout.CENTER);
        return section;
    }

    private static final class StatusBadgeRenderer extends JLabel implements TableCellRenderer {
        private Color badge = new Color(43, 63, 83);
        private Color ink = TEXT;
        private boolean selected;

        private StatusBadgeRenderer() { setHorizontalAlignment(SwingConstants.CENTER); setFont(new Font("SansSerif",Font.BOLD,12)); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            String status=value==null?"":value.toString().trim().toUpperCase();
            selected=isSelected;
            if(status.contains("REJECT") || status.contains("OVERDUE") || status.contains("HIGH") || status.contains("NEEDS_CHANGES")) {
                badge=new Color(91,43,54); ink=new Color(255,174,181);
            } else if(status.contains("APPROV") || status.contains("COMPLETED") || status.equals("LOW") || status.contains("APPROVED")) {
                badge=new Color(27,78,68); ink=new Color(123,240,202);
            } else if(status.contains("PENDING") || status.contains("TODO") || status.contains("MEDIUM") || status.contains("IN_PROGRESS")) {
                badge=new Color(86,67,39); ink=new Color(255,216,145);
            } else { badge=new Color(43,63,83); ink=TEXT; }
            setText(value==null?"":value.toString());
            setToolTipText(value==null?null:value.toString());
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected?new Color(39,68,103):SURFACE); g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(selected?new Color(52,86,123):badge);
            int width=Math.max(0,getWidth()-24), height=Math.max(0,getHeight()-14);
            g.fillRoundRect(12,7,width,height,14,14); g.dispose();
            setForeground(ink); super.paintComponent(graphics);
        }
    }

    public static void styleForm(Container container) {
        container.setBackground(SURFACE);
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label) {
                label.setForeground(TEXT);
                label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            } else if (component instanceof JTextComponent field) {
                field.setBackground(CARD);
                field.setForeground(TEXT);
                field.setCaretColor(GREEN);
                field.setFont(BODY);
                field.setPreferredSize(new Dimension(210, 38));
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER, 1, true),
                        new EmptyBorder(8, 11, 8, 11)));
            } else if (component instanceof JComboBox<?> combo) {
                styleComboBox(combo);
                combo.setPreferredSize(new Dimension(210, 38));
            } else if (component instanceof JSpinner spinner) {
                spinner.setFont(BODY);
                spinner.setPreferredSize(new Dimension(120, 38));
                JComponent editor = spinner.getEditor();
                if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
                    defaultEditor.getTextField().setBackground(CARD);
                    defaultEditor.getTextField().setForeground(TEXT);
                    defaultEditor.getTextField().setCaretColor(GREEN);
                }
            } else if (component instanceof Container child) {
                styleForm(child);
            }
        }
    }

    /** Shows an add/edit form in a dark, padded dialog with matching action buttons. */
    public static int showFormDialog(Component parent, Container form, String title) {
        styleForm(form);
        if (form instanceof JComponent component) {
            component.setBorder(new EmptyBorder(8, 10, 8, 10));
        }

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(12, 14, 8, 14));
        JScrollPane formScroll = new JScrollPane(form,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(SURFACE);
        formScroll.setPreferredSize(formDialogSize(form));
        content.add(formScroll, BorderLayout.CENTER);

        JOptionPane pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.getContentPane().setBackground(SURFACE);
        dialog.setResizable(true);
        styleDialogComponents(dialog.getContentPane());
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, 240));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        dialog.dispose();

        Object result = pane.getValue();
        return result instanceof Integer value ? value : JOptionPane.CLOSED_OPTION;
    }

    /** Reflows a live form dialog after a role-specific card changes. */
    public static void refreshFormDialog(Container form) {
        form.revalidate();
        form.repaint();
        Container current = form.getParent();
        while (current != null && !(current instanceof JScrollPane)) current = current.getParent();
        Window window = SwingUtilities.getWindowAncestor(form);
        if (current instanceof JScrollPane scroll) scroll.setPreferredSize(formDialogSize(form));
        if (window != null) {
            window.pack();
            window.setLocationRelativeTo(window.getOwner());
        }
    }

    private static Dimension formDialogSize(Container form) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension content = form.getPreferredSize();
        int maxWidth = Math.max(420, screen.width - 90);
        int maxHeight = Math.max(300, screen.height - 140);
        return new Dimension(Math.min(maxWidth, Math.max(460, content.width + 70)),
                Math.min(maxHeight, Math.max(180, content.height + 45)));
    }

    private static void styleDialogComponents(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) {
                String label = button.getText();
                styleButton(button, "OK".equalsIgnoreCase(label) ? GREEN : BLUE);
            } else if (component instanceof JScrollPane scroll) {
                scroll.getViewport().setBackground(CARD);
                scroll.setBorder(new LineBorder(BORDER, 1, true));
            } else if (component instanceof Container child) {
                styleDialogComponents(child);
            }
        }
    }
}

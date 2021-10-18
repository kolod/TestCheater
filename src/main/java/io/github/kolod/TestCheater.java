// This file is part of TestCheater.
// Copyright (C) 2019 - ... Oleksandr Kolodkin <alexandr.kolodkin@gmail.com>
//
// TestCheater is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Foobar is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
package io.github.kolod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drjekyll.fontchooser.FontDialog;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class TestCheater extends JFrame {

    private static final Logger logger = LogManager.getLogger();
    private static final FlatlafThemesComboBoxModel themesModel = new FlatlafThemesComboBoxModel();
    private static final Preferences prefs = Preferences.userNodeForPackage(TestCheater.class);
    private static Connection conn = null;
    
    // UI
    private final ResourceBundle bundle = ResourceBundle.getBundle("i18n/TestCheater");
    private final JLabel lblTest = new JLabel();
    private final JComboBox<String> test = new JComboBox<>();
    private final JLabel lblQuery = new JLabel();
    private final JButton btnFont = new JButton();
    private final JTextField query = new JTextField();
    private final JLabel lblAnswers = new JLabel();
    private final JScrollPane jScrollPane2 = new JScrollPane();
    private final JTable answers = new JTable();
    private final JButton btnClear = new JButton();
    private final JCheckBox mute = new JCheckBox();
    private final JComboBox<String> theme = new JComboBox<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Create and display the form */
        //FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new TestCheater().setVisible(true));
    }

    /**
     * Creates new form TestCheater
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public TestCheater() {

        // Restore font
        setCustomFont(new Font(
                prefs.get("FontName", "Segoe UI"),
                prefs.getInt("FontStyle", Font.BOLD),
                prefs.getInt("FontSize", 16)
        ));

        // Init components generated from form file
        initComponents();

        // Show maximized
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        // Customize JTable
        DefaultTableModel answersModel = new DefaultTableModel();
        answersModel.addColumn(bundle.getString("column_question"));
        answersModel.addColumn(bundle.getString("column_answer"));
        answers.setModel(answersModel);
        answers.getColumnModel().getColumn(0).setCellRenderer(new WordWrapCellRenderer());
        answers.getColumnModel().getColumn(1).setCellRenderer(new WordWrapCellRenderer());

        // Restore theme
        theme.setSelectedItem(prefs.get("theme", "Flat Light"));

        // Main logic
        connect();
        updateTestsList();
        updateAnswers();
        setRussianKeyboardLayout();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    logger.catching(ex);
                }
            }
        });

        theme.addActionListener(e -> {
            Object themeName = theme.getSelectedItem();
            if (themeName instanceof String) {
                prefs.put("theme", (String) themeName);
            }
        });

        test.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateAnswers();
            }
        });

        Action clearAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                query.setText("");
                query.requestFocus();
            }
        };

        btnClear.addActionListener(clearAction);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(112, 0, false), "Clear");
        getRootPane().getActionMap().put("Clear", clearAction);

        btnFont.addActionListener((ActionEvent e) -> {
            FontDialog dialog = new FontDialog(this, bundle.getString("title_choose_font"), true);
            dialog.setSelectedFont(btnFont.getFont());
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            if (!dialog.isCancelSelected()) {
                try {
                    Font font = dialog.getSelectedFont();
                    setCustomFont(font);
                    prefs.put("FontName", font.getFontName());
                    prefs.putInt("FontSize", font.getSize());
                    prefs.putInt("FontStyle", font.getStyle());
                    prefs.flush();
                } catch (BackingStoreException ex) {
                    logger.catching(ex);
                }
            }
        });

        query.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAnswers();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAnswers();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateAnswers();
            }
        });

        query.requestFocus();
    }

    private void setCustomFont(Font font) {
        UIManager.put("defaultFont", font);
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }
    }

    /**
     *
     */
    private void buzzer() {
        if (!mute.isSelected()) {
            AudioInputStream ais = null;
            try {
                InputStream stream = getClass().getResourceAsStream("buzzer.wav");
                if (stream != null) {
                    InputStream bufferedStream = new BufferedInputStream(stream);
                    ais = AudioSystem.getAudioInputStream(bufferedStream);
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.setFramePosition(0);
                    clip.start();
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                try {
                    if (ais != null) {
                        ais.close();
                    }
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Updates the list of tests
     */
    private void updateTestsList() {
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(new String[]{});

        if (conn != null) {
            String sql = "SELECT * FROM tests";
            try {
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    comboBoxModel.addElement(rs.getString("name"));
                }
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        test.setModel(comboBoxModel);
    }

    /**
     * Updates answers
     */
    private void updateAnswers() {
        DefaultTableModel answersModel = (DefaultTableModel) answers.getModel();

        while (answersModel.getRowCount() > 0) {
            answersModel.removeRow(0);
        }

        if (conn != null) {
            String question = this.query.getText().trim().toUpperCase();
            int testId = test.getSelectedIndex();

            if (question.isEmpty() && (testId >= 0)) {
                String SQL
                        = "SELECT q.text AS Вопрос, a.text AS Ответ "
                        + "FROM questions AS q "
                        + "LEFT JOIN answers AS a ON q.answer == a.id "
                        + "WHERE q.test == ? "
                        + "ORDER BY Вопрос";
                try {
                    try (PreparedStatement ps = conn.prepareStatement(SQL)) {
                        ps.setInt(1, testId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String[] row = {rs.getString("Вопрос"), rs.getString("Ответ")};
                                answersModel.addRow(row);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else {
                String SQL
                        = "SELECT q.text AS Вопрос, a.text AS Ответ "
                        + "FROM questions AS q "
                        + "LEFT JOIN answers AS a ON q.answer == a.id "
                        + "WHERE q.test == ? AND q.question_upper LIKE ? "
                        + "ORDER BY Вопрос";
                try {
                    try (PreparedStatement ps = conn.prepareStatement(SQL)) {
                        ps.setInt(1, testId);
                        ps.setString(2, "%" + question + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String[] row = {rs.getString("Вопрос"), rs.getString("Ответ")};
                                answersModel.addRow(row);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }

        if (answersModel.getRowCount() == 0) {
            buzzer();
        }

        answersModel.fireTableDataChanged();
    }

    /**
     * Connect to the sqlite database
     */
    private static void connect() {
        try {
            String url = "jdbc:sqlite::resource:io/github/kolod/tests.sqlite"; // db parameters
            conn = DriverManager.getConnection(url); // create a connection to the database
            logger.info("Connection to SQLite has been established.");
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void setRussianKeyboardLayout() {
        this.getInputContext().selectInputMethod(new Locale("ru", "RU"));
    }

    private void translateUI() {
        setTitle(bundle.getString("title"));
        lblTest.setText(bundle.getString("label_test"));
        lblQuery.setText(bundle.getString("label_question"));
        btnFont.setText(bundle.getString("button_font"));
        lblAnswers.setText(bundle.getString("label_answers"));
        btnClear.setText(bundle.getString("button_clear"));
        mute.setText(bundle.getString("button_mute"));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("io/github/kolod/icon.png")));

        translateUI();

        theme.setModel(themesModel);
        jScrollPane2.setViewportView(answers);
        test.getAccessibleContext().setAccessibleName("");
        mute.setHorizontalTextPosition(SwingConstants.LEADING);

        SpringLayout layout = new SpringLayout();
        Container contentPane = getContentPane();
        contentPane.setLayout(layout);

        contentPane.add(lblTest);
        contentPane.add(test);
        contentPane.add(lblQuery);
        contentPane.add(btnFont);
        contentPane.add(query);
        contentPane.add(lblAnswers);
        contentPane.add(jScrollPane2);
        contentPane.add(btnClear);
        contentPane.add(mute);
        contentPane.add(theme);

        // labelTest
        layout.putConstraint(SpringLayout.WEST, lblTest, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.BASELINE, lblTest, 0, SpringLayout.BASELINE, btnFont);

        // btnFont
        layout.putConstraint(SpringLayout.NORTH, btnFont, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.EAST, btnFont, -5, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.WEST, btnFont, 0, SpringLayout.WEST, btnClear);

        // theme
        layout.putConstraint(SpringLayout.NORTH, theme, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.EAST, theme, -5, SpringLayout.WEST, btnFont);

        // test
        layout.putConstraint(SpringLayout.NORTH, test, 5, SpringLayout.SOUTH, btnFont);
        layout.putConstraint(SpringLayout.WEST, test, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, test, -5, SpringLayout.EAST, contentPane);

        // mute
        layout.putConstraint(SpringLayout.NORTH, mute, 5, SpringLayout.SOUTH, test);
        layout.putConstraint(SpringLayout.EAST, mute, -5, SpringLayout.EAST, contentPane);

        // lblQuery
        layout.putConstraint(SpringLayout.WEST, lblQuery, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.BASELINE, lblQuery, 0, SpringLayout.BASELINE, mute);

        // btnClear
        layout.putConstraint(SpringLayout.NORTH, btnClear, 5, SpringLayout.SOUTH, mute);
        layout.putConstraint(SpringLayout.EAST, btnClear, -5, SpringLayout.EAST, contentPane);

        // query
        layout.putConstraint(SpringLayout.NORTH, query, 5, SpringLayout.SOUTH, mute);
        layout.putConstraint(SpringLayout.WEST, query, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, query, -5, SpringLayout.WEST, btnClear);

        // lblAnswers
        layout.putConstraint(SpringLayout.NORTH, lblAnswers, 5, SpringLayout.SOUTH, query);
        layout.putConstraint(SpringLayout.WEST, lblAnswers, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, lblAnswers, -5, SpringLayout.EAST, contentPane);

        // jScrollPane2
        layout.putConstraint(SpringLayout.NORTH, jScrollPane2, 5, SpringLayout.SOUTH, lblAnswers);
        layout.putConstraint(SpringLayout.WEST, jScrollPane2, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, jScrollPane2, -5, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.SOUTH, jScrollPane2, -5, SpringLayout.SOUTH, contentPane);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Overridden to trick sizing to respect the min.
     */
    @Override
    public boolean isMinimumSizeSet() {
        return true;
    }


    /**
     * Overridden to adjust for insets if tricking.
     */
    @Override
    public Dimension getMinimumSize() {
        Dimension dim = super.getMinimumSize();
        Insets insets = getInsets();

        List<Integer> width = new ArrayList<>();
        width.add(lblTest.getWidth() + theme.getWidth() + btnFont.getWidth() + 10);
        width.add(test.getWidth() + 10);
        width.add(lblQuery.getWidth() + mute.getWidth() + 5);
        width.add(query.getWidth() + btnClear.getWidth() + 5);

        dim.width += insets.left + insets.right + Collections.max(width);
        dim.height += insets.bottom + insets.top + btnFont.getHeight() + btnClear.getHeight() + mute.getHeight() + lblAnswers.getHeight() + test.getHeight() + 150;

        return dim;
    }
}

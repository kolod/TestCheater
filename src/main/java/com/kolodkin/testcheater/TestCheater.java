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
package com.kolodkin.testcheater;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.sound.sampled.*;
import com.kolodkin.lafselector.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.drjekyll.fontchooser.FontDialog;

public class TestCheater extends javax.swing.JFrame {

    private static final Logger logger = LogManager.getLogger();
    private static final ThemesComboBoxModel themesModel = new ThemesComboBoxModel();
    private static Connection conn = null;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Create and display the form */
        SwingUtilities.invokeLater(() -> new TestCheater().setVisible(true));
    }

    /**
     * Creates new form TestCheater
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public TestCheater() {
        Preferences prefs = Preferences.userNodeForPackage(TestCheater.class);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/TestCheater");

        // Restore font
        setCustomFont(new Font(
                prefs.get("FontName", "Segoe UI"),
                prefs.getInt("FontStyle", Font.BOLD),
                prefs.getInt("FontSize", 16)
        ));

        // Init components generated from form file
        initComponents();

        // Show miximized
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
            Object thameName = theme.getSelectedItem();
            if (thameName instanceof String) {
                prefs.put("theme", (String) thameName);
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
                InputStream bufferedStream = new BufferedInputStream(stream);

                ais = AudioSystem.getAudioInputStream(bufferedStream);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.setFramePosition(0);
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                logger.catching(ex);
            } finally {
                try {
                    if (ais != null) {
                        ais.close();
                    }
                } catch (IOException ex) {
                    logger.catching(ex);
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
            String SQL = "SELECT * FROM tests";
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        comboBoxModel.addElement(rs.getString("name"));
                    }
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
            String url = "jdbc:sqlite::resource:com/kolodkin/testcheater/tests.sqlite"; // db parameters
            conn = DriverManager.getConnection(url); // create a connection to the database  
            logger.info("Connection to SQLite has been established.");
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void setRussianKeyboardLayout() {
        this.getInputContext().selectInputMethod(new Locale("ru", "RU"));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTest = new javax.swing.JLabel();
        test = new javax.swing.JComboBox<>();
        lblQuery = new javax.swing.JLabel();
        btnFont = new javax.swing.JButton();
        query = new javax.swing.JTextField();
        lblAnswers = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        answers = new javax.swing.JTable();
        btnClear = new javax.swing.JButton();
        mute = new javax.swing.JCheckBox();
        theme = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("i18n/TestCheater"); // NOI18N
        setTitle(bundle.getString("title")); // NOI18N
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("com/kolodkin/testcheater/icon.png")));

        lblTest.setText(bundle.getString("label_test")); // NOI18N

        lblQuery.setText(bundle.getString("label_question")); // NOI18N

        btnFont.setText(bundle.getString("button_font")); // NOI18N

        lblAnswers.setText(bundle.getString("label_answers")); // NOI18N

        answers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(answers);

        btnClear.setText(bundle.getString("button_clear")); // NOI18N

        mute.setText(bundle.getString("button_mute")); // NOI18N
        mute.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        theme.setModel(themesModel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(lblQuery, javax.swing.GroupLayout.PREFERRED_SIZE, 537, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(mute))
                    .addComponent(test, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTest, javax.swing.GroupLayout.PREFERRED_SIZE, 479, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(theme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFont))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(query)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClear))
                    .addComponent(lblAnswers, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnFont, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblTest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(theme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(test, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblQuery)
                    .addComponent(mute))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(query, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnClear))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAnswers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                .addContainerGap())
        );

        test.getAccessibleContext().setAccessibleName("");

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable answers;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnFont;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblAnswers;
    private javax.swing.JLabel lblQuery;
    private javax.swing.JLabel lblTest;
    private javax.swing.JCheckBox mute;
    private javax.swing.JTextField query;
    private javax.swing.JComboBox<String> test;
    private javax.swing.JComboBox<String> theme;
    // End of variables declaration//GEN-END:variables
}

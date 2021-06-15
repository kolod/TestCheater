// This file is part of TestCheater.
// Copyright (C) 2019 Aleksandr Kolodkin <alexandr.kolodkin@gmail.com>
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

import java.util.prefs.Preferences;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import javax.swing.UIManager;
import javax.swing.JFrame;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;

import say.swing.JFontChooser;

public class TestCheater extends javax.swing.JFrame {

    /**
     * Creates new form TestCheater
     */
    public TestCheater() {
        UIManager.getLookAndFeelDefaults().put("defaultFont", new Font("Segoe UI", Font.BOLD, 14));
        
        initComponents();
        
        DefaultTableModel answersModel = new DefaultTableModel();
        answersModel.addColumn("Вопрос");
        answersModel.addColumn("Ответ");
        answers.setModel(answersModel);
        answers.getColumnModel().getColumn(0).setCellRenderer(new WordWrapCellRenderer());
        answers.getColumnModel().getColumn(1).setCellRenderer(new WordWrapCellRenderer());
        
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        
        Preferences prefs = Preferences.userNodeForPackage(TestCheater.class);
        Font font = new Font(
            prefs.get("FontName", "Segoe UI"), 
            prefs.getInt("FontStyle", Font.BOLD), 
            prefs.getInt("FontSize", 16)
        );
        
        setCustomFont(font);
        
        connect();        
        updateTestsList();
        updateAnswers();
                
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
        
        test.addItemListener((ItemEvent event) -> {
            if (event.getStateChange() == ItemEvent.SELECTED) updateAnswers();
        });
    }
    
    private void setCustomFont(Font font) {
        setFont(font);
        lblAnswers.setFont(font);
        lblQuery.setFont(font);
        lblTest.setFont(font);
        test.setFont(font);
        query.setFont(font);
        answers.setFont(font);
        btnFont.setFont(font);
        btnClear.setFont(font);
        jScrollPane2.setFont(font);
    }
    
    private void buzzer() throws URISyntaxException {
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
            Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (ais != null) ais.close();
            } catch (IOException ex) {
                Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    /**
     * Updates the list of tests
     */
    private void updateTestsList() {
        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        
        if (conn != null) {
            String SQL = "SELECT * FROM tests";
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL); ResultSet rs = ps.executeQuery()) {
                    while(rs.next()){
                        comboBoxModel.addElement(rs.getString("name"));
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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
                String SQL = 
                    "SELECT q.text AS Вопрос, a.text AS Ответ " +
                    "FROM questions AS q " +
                    "LEFT JOIN answers AS a ON q.answer == a.id " +
                    "WHERE q.test == ? " +
                    "ORDER BY Вопрос";
                try {
                    try (PreparedStatement ps = conn.prepareStatement(SQL)) {
                        ps.setInt(1, testId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while(rs.next()){
                                String[] row = {rs.getString("Вопрос"), rs.getString("Ответ")};
                                answersModel.addRow(row);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
            } else {
                String SQL = 
                    "SELECT q.text AS Вопрос, a.text AS Ответ " +
                    "FROM questions AS q " +
                    "LEFT JOIN answers AS a ON q.answer == a.id " +
                    "WHERE q.test == ? AND q.question_upper LIKE ? " +
                    "ORDER BY Вопрос";
                try {
                    try (PreparedStatement ps = conn.prepareStatement(SQL)) {
                        ps.setInt(1, testId);
                        ps.setString(2, "%" + question + "%");                        
                        try (ResultSet rs = ps.executeQuery()) {
                            while(rs.next()){
                                String[] row = {rs.getString("Вопрос"), rs.getString("Ответ")};
                                answersModel.addRow(row);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
        
        if (answersModel.getRowCount() == 0) try {
            buzzer();
        } catch (URISyntaxException ex) {
            Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        answersModel.fireTableDataChanged();
    }
    
    
    /**
     * Connect to a sample database
     */
    private static Connection conn = null;
    private static void connect() {
        try {
            String url = "jdbc:sqlite::resource:com/kolodkin/testcheater/tests.sqlite"; // db parameters
            conn = DriverManager.getConnection(url); // create a connection to the database           
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException ex) {
            Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Test Cheater 06.2021");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("com/kolodkin/testcheater/icon.png")));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblTest.setText("Тест:");

        lblQuery.setText("Поиск вопроса (можно использовать % и _):");

        btnFont.setText("Шрифт");
        btnFont.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFontActionPerformed(evt);
            }
        });

        lblAnswers.setText("Ответы:");

        answers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(answers);

        btnClear.setText("Очистить");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(test, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFont))
                    .addComponent(lblQuery, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(query)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClear))
                    .addComponent(lblAnswers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTest)
                    .addComponent(btnFont))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(test, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblQuery)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(query, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnClear))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAnswers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
                .addContainerGap())
        );

        test.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFontActionPerformed
        JFontChooser dialog = new JFontChooser();
        dialog.setSelectedFont(btnFont.getFont());
        if (dialog.showDialog(this) == JFontChooser.OK_OPTION) {
            try {
                Font font = dialog.getSelectedFont();
                setCustomFont(font);
                
                Preferences prefs = Preferences.userNodeForPackage(TestCheater.class);
                prefs.put("FontName", font.getFontName());
                prefs.putInt("FontSize", font.getSize());
                prefs.putInt("FontStyle", font.getStyle());
                
                prefs.flush();
            } catch (BackingStoreException ex) {
                Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_btnFontActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        query.setText("");
        query.requestFocus();
    }//GEN-LAST:event_btnClearActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(TestCheater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TestCheater.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TestCheater.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TestCheater.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TestCheater.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new TestCheater().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable answers;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnFont;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblAnswers;
    private javax.swing.JLabel lblQuery;
    private javax.swing.JLabel lblTest;
    private javax.swing.JTextField query;
    private javax.swing.JComboBox<String> test;
    // End of variables declaration//GEN-END:variables
}

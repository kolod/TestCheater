package com.kolodkin.testcheater;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.UIManager;
import javax.swing.JFrame;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;


public class TestCheater extends javax.swing.JFrame {

    /**
     * Creates new form TestCheater
     */
    public TestCheater() {
        UIManager.getLookAndFeelDefaults().put("defaultFont", new Font("Segoe UI", Font.BOLD, 14));
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        
        initComponents();
        
        DefaultTableModel answersModel = new DefaultTableModel();
        answersModel.addColumn("Вопрос");
        answersModel.addColumn("Ответ");
        answers.setModel(answersModel);
        answers.getColumnModel().getColumn(0).setCellRenderer(new WordWrapCellRenderer());
        answers.getColumnModel().getColumn(1).setCellRenderer(new WordWrapCellRenderer());
        
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
            } catch (SQLException e) {
                System.out.println(e.getMessage());
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
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
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
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        
        answersModel.fireTableDataChanged();
    }
    
    
    /**
     * Connect to a sample database
     */
    private static Connection conn = null;
    private static void connect() {
        try {
            String url = "jdbc:sqlite:resources/tests.sqlite"; // db parameters
            conn = DriverManager.getConnection(url); // create a connection to the database           
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Test Cheater");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("icon.png")));

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

        jButton1.setText("Очистить");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
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
                        .addComponent(btnFont, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblQuery, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(query)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                    .addComponent(query, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAnswers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE)
                .addContainerGap())
        );

        test.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFontActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnFontActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        query.setText("");
        query.requestFocus();
    }//GEN-LAST:event_jButton1ActionPerformed

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
    private javax.swing.JButton btnFont;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblAnswers;
    private javax.swing.JLabel lblQuery;
    private javax.swing.JLabel lblTest;
    private javax.swing.JTextField query;
    private javax.swing.JComboBox<String> test;
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kolodkin.testcheater;

import javax.swing.JTextArea;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.JComponent;

/**
 *
 * @author alexa
 */
public class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {

    @Override
    public JComponent getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setLineWrap(true);
        setWrapStyleWord(true);
        setText(value.toString());
        setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
        setBorder(null);
        if (table.getRowHeight(row) < getPreferredSize().height) {
            table.setRowHeight(row, getPreferredSize().height);
        }
        return this;
    }
}

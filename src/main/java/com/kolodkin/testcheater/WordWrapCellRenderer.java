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

import java.awt.Color;
import javax.swing.JTextArea;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

public class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {
    
    @Override
    public JComponent getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setLineWrap(true);
        setWrapStyleWord(true);
        
        setText((value == null) ? "" : value.toString());
        setFont(table.getFont());  
        setSize(table.getColumnModel().getColumn(column).getWidth(), table.getRowHeight(row));
        
        int preferredHeight = getPreferredSize().height;
        if (table.getRowHeight(row) != preferredHeight) {
            table.setRowHeight(row, preferredHeight);
        }
        
        // Alternate row background
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Color normalColor = defaults.getColor("Table.background");
        Color alternateColor = defaults.getColor("Table.alternateRowColor");
        if (alternateColor == null) alternateColor = defaults.getColor("Table.alternateRowColor");
        if (alternateColor == null) alternateColor = defaults.getColor("Panel.background");
        if (alternateColor == null) alternateColor = normalColor;        
        setBackground((row % 2 == 0) ? normalColor : alternateColor);              
        
        return this;
    }
}

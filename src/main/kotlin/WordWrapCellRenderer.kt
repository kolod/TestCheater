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

package io.github.kolod

import java.awt.Color
import javax.swing.*
import javax.swing.table.TableCellRenderer

class WordWrapCellRenderer : JTextArea(), TableCellRenderer {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): JComponent {
        lineWrap = true
        wrapStyleWord = true
        text = value.toString().trim()
        font = table.font
        setSize(table.columnModel.getColumn(column).width, table.getRowHeight(row))

        // Calculate row height
        val preferredHeight: Int = preferredSize.height
        if ((column == 0) || (table.getRowHeight(row) < preferredHeight)) {
            table.setRowHeight(row, preferredHeight)
        }

        // Alternate row background
        val defaults: UIDefaults = UIManager.getLookAndFeelDefaults()
        val normalColor: Color = defaults.getColor("Table.background") ?: Color.WHITE
        val alternateColor: Color =
            defaults.getColor("Table.alternateRowColor") ?:
            defaults.getColor("Panel.background") ?:
            normalColor

        background = if (row % 2 == 0) normalColor else alternateColor
        return this
    }
}
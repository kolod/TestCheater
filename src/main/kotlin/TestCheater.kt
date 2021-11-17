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
package io.github.kolod

import com.jcabi.manifests.Manifests
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import org.apache.logging.log4j.LogManager
import org.drjekyll.fontchooser.FontDialog
import java.awt.Dimension
import java.awt.Font
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class TestCheater : JFrame() {
    private val logger = LogManager.getLogger()
    private val themesModel = FlatlafThemesComboBoxModel()
    private val prefs = Preferences.userNodeForPackage(TestCheater::class.java)
    private var conn: Connection? = null

    // UI
    private val bundle = ResourceBundle.getBundle("i18n/TestCheater")
    private val lblTest = JLabel()
    private val test = JComboBox<String>()
    private val lblQuery = JLabel()
    private val btnFont = JButton()
    private val query = JTextField()
    private val lblAnswers = JLabel()
    private val jScrollPane2 = JScrollPane()
    private val answers = JTable()
    private val btnClear = JButton()
    private val mute = JCheckBox()
    private val theme = JComboBox<String>()
    private val fxPanel = JFXPanel() // Init JavaFX

    /**
     * Connect to the sqlite database
     */
    private fun connect() {
        try {
            val url = "jdbc:sqlite::resource:io/github/kolod/tests.sqlite" // db parameters
            conn = DriverManager.getConnection(url) // create a connection to the database
            logger.info("Connection to SQLite has been established.")
        } catch (ex: SQLException) {
            logger.error(ex.message, ex)
        }
    }

    /**
     *
     */
    private fun setCustomFont(font: Font) {
        UIManager.put("defaultFont", font)
        getWindows().forEach { window -> SwingUtilities.updateComponentTreeUI(window) }
    }

    /**
     *
     */
    private fun buzzer() {
        val url: URL? = javaClass.getResource("buzzer.wav")
        val buzzer = Media(url?.toExternalForm())
        val mediaPlayer = MediaPlayer(buzzer)
        mediaPlayer.volume = 0.05
        mediaPlayer.play()
    }

    /**
     * Updates the list of tests
     */
    private fun updateTestsList() {
        val comboBoxModel = DefaultComboBoxModel(arrayOf<String>())
        if (conn != null) {
            val sql = "SELECT * FROM tests"
            try {
                val ps = conn!!.prepareStatement(sql)
                val rs = ps.executeQuery()
                while (rs.next()) {
                    comboBoxModel.addElement(rs.getString("name"))
                }
            } catch (ex: SQLException) {
                logger.error(ex.message, ex)
            }
        }
        test.model = comboBoxModel
    }

    /**
     * Updates answers
     */
    private fun updateAnswers() {
        val answersModel = answers.model as DefaultTableModel
        while (answersModel.rowCount > 0) answersModel.removeRow(0)
        if (conn != null) {
            val question = query.text.trim { it <= ' ' }.uppercase()
            val testId = test.selectedIndex
            if (question.isEmpty() && testId >= 0) {
                val sql = ("SELECT q.text AS Вопрос, a.text AS Ответ "
                        + "FROM questions AS q "
                        + "LEFT JOIN answers AS a ON q.answer == a.id "
                        + "WHERE q.test == ? "
                        + "ORDER BY Вопрос")
                try {
                    conn!!.prepareStatement(sql).use { ps ->
                        ps.setInt(1, testId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val row = arrayOf(rs.getString("Вопрос"), rs.getString("Ответ"))
                                answersModel.addRow(row)
                            }
                        }
                    }
                } catch (ex: SQLException) {
                    logger.error(ex.message, ex)
                }
            } else {
                val sql = ("SELECT q.text AS Вопрос, a.text AS Ответ "
                        + "FROM questions AS q "
                        + "LEFT JOIN answers AS a ON q.answer == a.id "
                        + "WHERE q.test == ? AND q.question_upper LIKE ? "
                        + "ORDER BY Вопрос")
                try {
                    conn!!.prepareStatement(sql).use { ps ->
                        ps.setInt(1, testId)
                        ps.setString(2, "%$question%")
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val row = arrayOf(rs.getString("Вопрос"), rs.getString("Ответ"))
                                answersModel.addRow(row)
                            }
                        }
                    }
                } catch (ex: SQLException) {
                    logger.error(ex.message, ex)
                }
            }
        }
        if (answersModel.rowCount == 0) {
            buzzer()
        }
        answersModel.fireTableDataChanged()
    }

    private fun setRussianKeyboardLayout() {
        inputContext.selectInputMethod(Locale("ru", "RU"))
    }

    private fun translateUI() {
        with(bundle) {
            title           = getString("title") + " " + Manifests.read("Build-Date")
            lblTest.text    = getString("label_test")
            lblQuery.text   = getString("label_question")
            btnFont.text    = getString("button_font")
            lblAnswers.text = getString("label_answers")
            btnClear.text   = getString("button_clear")
            mute.text       = getString("button_mute")
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private fun initComponents() {
        defaultCloseOperation = EXIT_ON_CLOSE
        iconImage = Toolkit.getDefaultToolkit().getImage(javaClass.classLoader.getResource("io/github/kolod/icon.png"))
        translateUI()

        theme.model = themesModel

        jScrollPane2.setViewportView(answers)
        test.accessibleContext.accessibleName = ""
        mute.horizontalTextPosition = SwingConstants.LEADING

        val layout = SpringLayout()
        val contentPane = contentPane
        contentPane.layout = layout
        contentPane.add(lblTest)
        contentPane.add(test)
        contentPane.add(lblQuery)
        contentPane.add(btnFont)
        contentPane.add(query)
        contentPane.add(lblAnswers)
        contentPane.add(jScrollPane2)
        contentPane.add(btnClear)
        contentPane.add(mute)
        contentPane.add(theme)

        // labelTest
        layout.putConstraint(SpringLayout.WEST, lblTest, 5, SpringLayout.WEST, contentPane)
        layout.putConstraint(SpringLayout.BASELINE, lblTest, 0, SpringLayout.BASELINE, btnFont)

        // btnFont
        layout.putConstraint(SpringLayout.NORTH, btnFont, 5, SpringLayout.NORTH, contentPane)
        layout.putConstraint(SpringLayout.EAST, btnFont, -5, SpringLayout.EAST, contentPane)
        layout.putConstraint(SpringLayout.WEST, btnFont, 0, SpringLayout.WEST, btnClear)

        // theme
        layout.putConstraint(SpringLayout.NORTH, theme, 5, SpringLayout.NORTH, contentPane)
        layout.putConstraint(SpringLayout.EAST, theme, -5, SpringLayout.WEST, btnFont)

        // test
        layout.putConstraint(SpringLayout.NORTH, test, 5, SpringLayout.SOUTH, btnFont)
        layout.putConstraint(SpringLayout.WEST, test, 5, SpringLayout.WEST, contentPane)
        layout.putConstraint(SpringLayout.EAST, test, -5, SpringLayout.EAST, contentPane)

        // mute
        layout.putConstraint(SpringLayout.NORTH, mute, 5, SpringLayout.SOUTH, test)
        layout.putConstraint(SpringLayout.EAST, mute, -5, SpringLayout.EAST, contentPane)

        // lblQuery
        layout.putConstraint(SpringLayout.WEST, lblQuery, 5, SpringLayout.WEST, contentPane)
        layout.putConstraint(SpringLayout.BASELINE, lblQuery, 0, SpringLayout.BASELINE, mute)

        // btnClear
        layout.putConstraint(SpringLayout.NORTH, btnClear, 5, SpringLayout.SOUTH, mute)
        layout.putConstraint(SpringLayout.EAST, btnClear, -5, SpringLayout.EAST, contentPane)

        // query
        layout.putConstraint(SpringLayout.NORTH, query, 5, SpringLayout.SOUTH, mute)
        layout.putConstraint(SpringLayout.WEST, query, 5, SpringLayout.WEST, contentPane)
        layout.putConstraint(SpringLayout.EAST, query, -5, SpringLayout.WEST, btnClear)

        // lblAnswers
        layout.putConstraint(SpringLayout.NORTH, lblAnswers, 5, SpringLayout.SOUTH, query)
        layout.putConstraint(SpringLayout.WEST, lblAnswers, 5, SpringLayout.WEST, contentPane)
        layout.putConstraint(SpringLayout.EAST, lblAnswers, -5, SpringLayout.EAST, contentPane)

        // jScrollPane2
        layout.putConstraint(SpringLayout.NORTH, jScrollPane2, 5, SpringLayout.SOUTH, lblAnswers)
        layout.putConstraint(SpringLayout.WEST, jScrollPane2, 5, SpringLayout.WEST, contentPane)
        layout.putConstraint(SpringLayout.EAST, jScrollPane2, -5, SpringLayout.EAST, contentPane)
        layout.putConstraint(SpringLayout.SOUTH, jScrollPane2, -5, SpringLayout.SOUTH, contentPane)
        pack()
        setLocationRelativeTo(null)

        theme.selectedItem = "Arc"
        theme.selectedItem = prefs["theme", "Flat Light"] // Restore theme
    }

    /**
     * Overridden to trick sizing to respect the min.
     */
    override fun isMinimumSizeSet(): Boolean = true

    /**
     * Overridden to adjust for insets if tricking.
     */
    override fun getMinimumSize(): Dimension = super.getMinimumSize().apply {
        width = insets.left + insets.right + (arrayOf (
            lblTest.width + theme.width + btnFont.width + 10,
            test.width + 10,
            lblQuery.width + mute.width + 5,
            query.width + btnClear.width + 5
        ).maxOrNull() ?: 0)

        height += insets.bottom + insets.top + btnFont.height + btnClear.height +
                mute.height + lblAnswers.height + test.height + 150
    }

    /**
     * Creates new form TestCheater
     */
    init {

        // Restore font
        setCustomFont(
            Font(
                prefs["FontName", "Segoe UI"],
                prefs.getInt("FontStyle", Font.BOLD),
                prefs.getInt("FontSize", 16)
            )
        )

        // Init components generated from form file
        initComponents()

        // Show maximized
        extendedState = extendedState or MAXIMIZED_BOTH

        // Customize JTable
        val answersModel = DefaultTableModel()
        answersModel.addColumn(bundle.getString("column_question"))
        answersModel.addColumn(bundle.getString("column_answer"))
        answers.model = answersModel
        answers.columnModel.getColumn(0).cellRenderer = WordWrapCellRenderer()
        answers.columnModel.getColumn(1).cellRenderer = WordWrapCellRenderer()

        // Main logic
        connect()
        updateTestsList()
        updateAnswers()
        setRussianKeyboardLayout()

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(evt: WindowEvent) {
                try {
                    conn!!.close()
                } catch (ex: SQLException) {
                    logger.catching(ex)
                }
            }
        })

        theme.addActionListener {
            val themeName = theme.selectedItem
            if (themeName is String) prefs.put("theme", themeName)
        }

        test.addItemListener { e: ItemEvent ->
            if (e.stateChange == ItemEvent.SELECTED) updateAnswers()
        }

        val clearAction: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                query.text = ""
                query.requestFocus()
            }
        }

        btnClear.addActionListener(clearAction)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(112, 0, false), "Clear")
        getRootPane().actionMap.put("Clear", clearAction)

        btnFont.addActionListener {
            val dialog = FontDialog(this, bundle.getString("title_choose_font"), true)
            dialog.selectedFont = btnFont.font
            dialog.setLocationRelativeTo(this)
            dialog.isVisible = true
            if (!dialog.isCancelSelected) try {
                with (dialog.selectedFont) {
                    setCustomFont(this)
                    prefs.put("FontName", fontName)
                    prefs.putInt("FontSize", size)
                    prefs.putInt("FontStyle", style)
                    prefs.flush()
                }
            } catch (ex: BackingStoreException) {
                logger.catching(ex)
            }
        }

        query.requestFocus()
        query.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateAnswers()
            override fun removeUpdate(e: DocumentEvent) = updateAnswers()
            override fun changedUpdate(e: DocumentEvent) = updateAnswers()
        })
    }

    companion object {
        /**
         * @param args the command line arguments
         */
        @JvmStatic
        fun main(args: Array<String>) {
            /* Create and display the form */
            SwingUtilities.invokeLater { TestCheater().isVisible = true }
        }
    }
}
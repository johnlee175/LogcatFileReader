/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.johnsoft.logcat.LogCatFilter;
import com.johnsoft.logcat.LogLevel;
import com.johnsoft.logcat.LogicalPredicate;

/**
 * @author John Kenrinus Lee
 * @version 2016-04-25
 */
public class LogTableView extends JTable {
    private static final Color VERBOSE_COLOR = new Color(25, 25, 25);
    private static final Color DEBUG_COLOR = new Color(0, 0, 200);
    private static final Color INFO_COLOR = new Color(0, 100, 0);
    private static final Color WARN_COLOR = new Color(200, 125, 0);
    private static final Color ERROR_COLOR = new Color(150, 0, 0);
    private static final Color ASSERT_COLOR = new Color(200, 125, 125);

    private JComboBox<LogLevel> levelSelector;
    private JComboBox<LogicalPredicate> logicalSelector;
    private JTextField messageFilter;

    public LogTableView() {
        init();
        createComboBox();
        createTextField();
    }

    public JComboBox<LogLevel> getLevelSelector() {
        return levelSelector;
    }

    public JComboBox<LogicalPredicate> getLogicalSelector() {
        return logicalSelector;
    }

    public JTextField getMessageFilter() {
        return messageFilter;
    }

    public void doFindAction(boolean findNextOne, String findingText,
                             boolean matchCase, boolean regex, JLabel resultDescription) {
        int row = getSelectedRow();
        if (row < 0) {
            row = rowAtPoint(getVisibleRect().getLocation());
        } else {
            if (findNextOne) {
                ++row;
            } else {
                --row;
            }
        }
        int idx = ((LogTableModel)getModel()).doFind(row, findNextOne, findingText, matchCase, regex);
        if (idx < 0 && resultDescription != null) {
            resultDescription.setText("no matches");
            return;
        }
        selectRow(idx);
        if (resultDescription != null && ((findNextOne && idx <= row) || (!findNextOne && idx >= row))) {
            resultDescription.setText("wrapped search");
        }
    }

    private void selectRow(int index) {
        final int rowCount = getRowCount();
        if (index >= rowCount || index < 0) {
            return;
        }
        final int halfRegion = 10;
        int up = index - halfRegion;
        int down = index + halfRegion;
        if (up < 0) {
            up = 0;
        }
        if (down >= rowCount) {
            down = rowCount - 1;
        }
        final Rectangle upHalf = getCellRect(up, 0, true);
        final Rectangle downHalf = getCellRect(down, 0, true);
        Rectangle visible = new Rectangle();
        Rectangle.union(upHalf, downHalf, visible);
        scrollRectToVisible(visible);
        setRowSelectionInterval(index, index);
    }

    private void init() {
        setModel(new LogTableModel());
        setDefaultRenderer(String.class, new SingleLineCellRenderer());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setFillsViewportHeight(true);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(0, 1));
        setAutoResizeMode(AUTO_RESIZE_OFF);
        getTableHeader().setReorderingAllowed(false);
        configColumn("Level", true, 50);
        configColumn("Time", true, 150);
        configColumn("PID", true, 75);
        configColumn("TID", true, 75);
        configColumn("Application", true, 75);
        configColumn("Thread", true, 50);
        TableColumn textColumn = getColumn("Text");
        textColumn.setMinWidth(500);
        textColumn.setPreferredWidth(800);
        textColumn.setCellRenderer(new MultiLineCellRenderer());

        getTableHeader().addMouseListener(new MouseAdapter() {
            private final String prefixText = "hide column";
            private final JPopupMenu popupMenu = new JPopupMenu();
            private final JMenuItem hideColumn = popupMenu.add(new JMenuItem(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String[] strings = hideColumn.getText().split("\\s+");
                    if (strings.length > 0) {
                        final String identifier = strings[strings.length - 1];
                        if (identifier != null && !identifier.trim().isEmpty()) {
                            // hide column
                            TableColumnModel columnModel = getColumnModel();
                            int columnIdx = columnModel.getColumnIndex(identifier);
                            TableColumn column = columnModel.getColumn(columnIdx);
                            column.setPreferredWidth(0);
                            column.setMinWidth(0);
                            column.setMaxWidth(0);
                            column = getTableHeader().getColumnModel().getColumn(columnIdx);
                            column.setPreferredWidth(0);
                            column.setMinWidth(0);
                            column.setMaxWidth(0);
                        }
                    }
                }
            }));
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {//right click
                    final int columnIdx = getTableHeader().columnAtPoint(e.getPoint());
                    final TableColumnModel columnModel = getColumnModel();
                    hideColumn.setText(prefixText + " " + columnModel.getColumn(columnIdx).getIdentifier());
                    popupMenu.show(getTableHeader(), e.getX(), e.getY());
                }
            }
        });
    }

    private void createComboBox() {
        levelSelector = new JComboBox<>(new LogLevel[] {LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO,
                LogLevel.WARN, LogLevel.ERROR, LogLevel.ASSERT});
        levelSelector.setEditable(false);
        levelSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    doFilter();
                }
            }
        });
        logicalSelector = new JComboBox<>(new LogicalPredicate[] {LogicalPredicate.AND, LogicalPredicate.OR});
        logicalSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    doFilter();
                }
            }
        });
        logicalSelector.setEditable(false);
    }

    private void createTextField() {
        messageFilter = new JTextField();
        messageFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                doFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                doFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                System.out.println("changedUpdate");
            }
        });
    }

    private void doFilter() {
        List<LogCatFilter> logCatFilters = LogCatFilter.fromString(messageFilter.getText(),
                (LogLevel) levelSelector.getSelectedItem());
        if (logCatFilters != null && !logCatFilters.isEmpty()) {
            ((LogTableModel) getModel()).setRowFilter(logCatFilters,
                    (LogicalPredicate) logicalSelector.getSelectedItem());
        }
    }

    private void configColumn(Object identifer, boolean resizable, int preferredWidth) {
        TableColumn column = getColumn(identifer);
        column.setResizable(resizable);
        column.setPreferredWidth(preferredWidth);
    }

    private static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(String.valueOf(value));
            setLineWrap(true);
            setWrapStyleWord(true);

            int maxPreferredHeight = 20;
            for (int i = 0; i < table.getColumnCount(); i++) {
                setSize(table.getColumnModel().getColumn(column).getWidth(), 0);
                maxPreferredHeight = Math.max(maxPreferredHeight, getPreferredSize().height);
            }
            if (table.getRowHeight(row) != maxPreferredHeight) {
                table.setRowHeight(row, maxPreferredHeight);
            }

            applyCellStyle(this, table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    private static class SingleLineCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(String.valueOf(value));
            setVerticalAlignment(SwingConstants.TOP);

            applyCellStyle(this, table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    private static void applyCellStyle(JComponent component, JTable table,
                                       Object value, boolean isSelected, boolean hasFocus,
                                       int row, int column) {
        if (isSelected) {
            component.setBackground(table.getSelectionBackground());
            component.setForeground(table.getSelectionForeground());
        } else {
            component.setBackground(table.getBackground());
            String level = ((LogTableModel)table.getModel()).getLogLevel(row);
            if ("E".equals(level)) {
                component.setForeground(ERROR_COLOR);
            } else if ("W".equals(level)) {
                component.setForeground(WARN_COLOR);
            } else if ("I".equals(level)) {
                component.setForeground(INFO_COLOR);
            } else if ("D".equals(level)) {
                component.setForeground(DEBUG_COLOR);
            } else if ("A".equals(level)) {
                component.setForeground(ASSERT_COLOR);
            } else {
                component.setForeground(VERBOSE_COLOR);
            }
        }
    }
}



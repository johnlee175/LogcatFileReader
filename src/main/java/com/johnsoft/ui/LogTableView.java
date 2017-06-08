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

import com.johnsoft.logcat.LogCatFilter;
import com.johnsoft.logcat.LogCatFilterImpl;
import com.johnsoft.logcat.LogLevel;
import com.johnsoft.logcat.LogicalPredicate;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

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
    private JTextField gotoLine;

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

    public JTextField getGotoLine() {
        return gotoLine;
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
        selectRow(this, idx);
        if (resultDescription != null && ((findNextOne && idx <= row) || (!findNextOne && idx >= row))) {
            resultDescription.setText("wrapped search");
        }
    }

    private static void selectRow(JTable table, int index) {
        final int rowCount = table.getRowCount();
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
        if (down < up) {
            return;
        }
        final Rectangle upHalf = table.getCellRect(up, 0, true);
        final Rectangle downHalf = table.getCellRect(down, 0, true);
        Rectangle visible = new Rectangle();
        Rectangle.union(upHalf, downHalf, visible);
        table.scrollRectToVisible(visible);
        table.setRowSelectionInterval(index, index);
    }

    private static void initDefaults(final JTable table) {
        table.setDefaultRenderer(String.class, new SingleLineCellRenderer());
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoResizeMode(AUTO_RESIZE_OFF);
        table.setAutoscrolls(true);
        table.setRowHeight(calcRowHeight(table));
        table.getTableHeader().setReorderingAllowed(false);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
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
                            TableColumnModel columnModel = table.getColumnModel();
                            int columnIdx = columnModel.getColumnIndex(identifier);
                            TableColumn column = columnModel.getColumn(columnIdx);
                            column.setPreferredWidth(0);
                            column.setMinWidth(0);
                            column.setMaxWidth(0);
                            column = table.getTableHeader().getColumnModel().getColumn(columnIdx);
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
                    final int columnIdx = table.getTableHeader().columnAtPoint(e.getPoint());
                    final TableColumnModel columnModel = table.getColumnModel();
                    hideColumn.setText(prefixText + " " + columnModel.getColumn(columnIdx).getIdentifier());
                    popupMenu.show(table.getTableHeader(), e.getX(), e.getY());
                }
            }
        });
    }

    private static int calcRowHeight(JTable table) {
        final Font font = table.getFont();
        final FontMetrics fm = table.getFontMetrics(font);
        final int ascent = fm.getAscent();
        final int descent = fm.getDescent();
        final int margin = font.getSize() / 2;
        return ascent + descent + margin;
    }

    private void init() {
        setModel(new LogTableModel());
        initDefaults(this);

        addMouseListener(new MouseAdapter() {
            private final String prefixText = "peek context surround";
            private final JPopupMenu popupMenu = new JPopupMenu();
            private final JMenuItem peekContext = popupMenu.add(new JMenuItem(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final LogTableModel.SubLogTableModel subModel =((LogTableModel)getModel()).subView(modelRow, 250);
                    Window window = (Window) LogTableView.this.getTopLevelAncestor();
                    if (subModel != null) {
                        final JTable subTable = new JTable(subModel);
                        initDefaults(subTable);
                        JDialog peeContextTip = new JDialog(window, "SubContextView[" + modelRow + "]");
                        peeContextTip.setContentPane(new JScrollPane(subTable));
                        peeContextTip.setSize(1000, 500);
                        peeContextTip.setMinimumSize(new Dimension(600, 200));
                        peeContextTip.setLocationRelativeTo(null);
                        peeContextTip.setVisible(true);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                selectRow(subTable, subModel.getFrom());
                            }
                        });
                    } else {
                        JOptionPane.showMessageDialog(window, "Can't build sub context view",
                                "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }));
            private int modelRow;

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {//right click
                    int row = getSelectedRow();
                    if (row < 0) {
                        row = rowAtPoint(e.getPoint());
                        if (row < 0) {
                            return;
                        }
                    }
                    modelRow = ((LogTableModel)getModel()).getModelRowIndex(row);
                    if (modelRow >= 0) {
                        peekContext.setText(prefixText + " " + modelRow);
                        popupMenu.show(LogTableView.this, e.getX(), e.getY());
                    }
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
        gotoLine = new JTextField(4);
        gotoLine.enableInputMethods(false);
        gotoLine.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = gotoLine.getText();
                try {
                    int number = Integer.parseInt(text);
                    selectRow(LogTableView.this, number);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });
        gotoLine.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (gotoLine.getText().isEmpty()) {
                    return;
                }
                final int offset = e.getOffset();
                final int len = e.getLength();
                if (!gotoLine.getText().matches("^[0-9]*[1-9][0-9]*$")) {
                    clear(offset, len);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (gotoLine.getText().isEmpty()) {
                    return;
                }
                final int offset = e.getOffset();
                final int len = e.getLength();
                if (!gotoLine.getText().matches("^[0-9]*[1-9][0-9]*$")) {
                    clear(offset, len);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                System.out.println("changedUpdate");
            }

            private void clear(final int offset, final int len) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Document document = gotoLine.getDocument();
                            final int length = document.getLength();
                            document.remove(Math.max(0, offset), Math.min(len, length));
                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void doFilter() {
        List<LogCatFilter> logCatFilters = LogCatFilterImpl.fromString(messageFilter.getText(),
                (LogLevel) levelSelector.getSelectedItem());
        if (logCatFilters != null && !logCatFilters.isEmpty()) {
            ((LogTableModel) getModel()).setRowFilter(logCatFilters,
                    (LogicalPredicate) logicalSelector.getSelectedItem());
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

//            final int width = table.getFontMetrics(table.getFont()).stringWidth(String.valueOf(value));
            final int width = getPreferredSize().width;
            final TableColumn tc = table.getColumnModel().getColumn(column);
            if (width > tc.getWidth()) {
                tc.setPreferredWidth(width);
            }
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
            String level = ((CommonModel)table.getModel()).getLogLevel(row);
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

    public interface CommonModel {
        String getLogLevel(int row);
    }
}



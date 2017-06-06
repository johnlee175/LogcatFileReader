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

import com.johnsoft.logcat.LogCatMessage;
import com.johnsoft.logcat.LogCatMessageParser;
import com.johnsoft.logcat.LogCatMessageParser2;
import com.johnsoft.logcat.LogCatMessageParser3;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author John Kenrinus Lee
 * @version 2016-04-25
 */
public class Reader {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String[] MESSAGES = new String[] {
            "[ 08-11 19:11:07.132   495:0x1ef D/dtag     ]", //$NON-NLS-1$
            "debug message",                                 //$NON-NLS-1$
            "[ 08-11 19:11:07.132   495:  234 E/etag     ]", //$NON-NLS-1$
            "error message",                                 //$NON-NLS-1$
            "[ 08-11 19:11:07.132   495:0x1ef I/itag     ]", //$NON-NLS-1$
            "info message",                                  //$NON-NLS-1$
            "[ 08-11 19:11:07.132   495:0x1ef V/vtag     ]", //$NON-NLS-1$
            "verbose message",                               //$NON-NLS-1$
            "[ 08-11 19:11:07.132   495:0x1ef W/wtag     ]", //$NON-NLS-1$
            "warning message",                               //$NON-NLS-1$
            "[ 08-11 19:11:07.132   495:0x1ef F/wtftag   ]", //$NON-NLS-1$
            "wtf message",                                   //$NON-NLS-1$
            "[ 08-11 21:15:35.7524  540:0x21c D/dtag     ]", //$NON-NLS-1$
            "debug message",                                 //$NON-NLS-1$
    };

    private static final class DragSupport extends MouseAdapter {
        private int startX, startY;
        private Component component;

        public void setTarget(Component component) {
            this.component = component;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                startX = e.getXOnScreen();
                startY = e.getYOnScreen();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && component != null) {
                int endX = e.getXOnScreen();
                int endY = e.getYOnScreen();
                Point location  = component.getLocationOnScreen();
                component.setLocation(location.x + (endX - startX), location.y + (endY - startY));
                startX = endX;
                startY = endY;
            }
        }
    }

    private LogTableModel logTableModel;
    private LogTableView tableView;
    private JFrame jFrame;

    private void registerFindShotcutAction() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            private JDialog dialog;
            private Component focusableNode;
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event.getID() == KeyEvent.KEY_PRESSED) {
                    KeyEvent keyEvent = (KeyEvent)event;
                    final int keyCode = keyEvent.getKeyCode();
                    if (keyCode == KeyEvent.VK_F &&
                            (OS.startsWith("mac") ? keyEvent.isMetaDown() : keyEvent.isControlDown())) {
                        if (tableView == null || !tableView.isVisible() || !tableView.isShowing()) {
                            return;
                        }
                        if (dialog != null && dialog.isVisible() && dialog.isShowing()) {
                            dialog.requestFocus();
                            if (focusableNode != null) {
                                focusableNode.requestFocusInWindow();
                            }
                            return;
                        }
                        dialog = new JDialog(jFrame);
                        dialog.setUndecorated(true);
                        dialog.setAlwaysOnTop(true);
                        final DragSupport dragSupport = new DragSupport();
                        dragSupport.setTarget(dialog);
                        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                        panel.setBackground(Color.WHITE);
                        panel.addMouseListener(dragSupport);
                        panel.addMouseMotionListener(dragSupport);
                        JLabel hand = new JLabel(new ImageIcon(getClass().getResource("/icons/hand.png")));
                        hand.addMouseListener(dragSupport);
                        hand.addMouseMotionListener(dragSupport);
                        JLabel close = new JLabel(new ImageIcon(getClass().getResource("/icons/close.png")));
                        close.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                focusableNode = null;
                                dialog.dispose();
                            }
                        });
                        final JTextField text = new JTextField(20);
                        final JCheckBox matchCase = new JCheckBox("Match Case");
                        final JCheckBox regex = new JCheckBox("Regex");
                        focusableNode = text;
                        final JLabel description = new JLabel("", SwingConstants.CENTER);
                        description.setPreferredSize(new Dimension(300, 30));
                        JButton next = new JButton(new ImageIcon(getClass().getResource("/icons/down.png")));
                        next.setPreferredSize(new Dimension(32, 32));
                        next.setActionCommand("Next");
                        next.setBorderPainted(false);
                        next.setFocusPainted(false);
                        next.setContentAreaFilled(false);
                        JButton prev = new JButton(new ImageIcon(getClass().getResource("/icons/up.png")));
                        prev.setPreferredSize(new Dimension(32, 32));
                        prev.setActionCommand("Prev");
                        prev.setBorderPainted(false);
                        prev.setFocusPainted(false);
                        prev.setContentAreaFilled(false);
                        final ActionListener actionListener = new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                final String action = e.getActionCommand();
                                final boolean findNextOne;
                                if ("Next".equals(action)) {
                                    findNextOne = true;
                                } else if ("Prev".equals(action)) {
                                    findNextOne = false;
                                } else {
                                    System.err.println("Unknown action command!");
                                    return;
                                }
                                String findingText = text.getText();
                                boolean applyMatchCase = matchCase.isSelected();
                                boolean applyRegex = regex.isSelected();
                                if (tableView != null) {
                                    description.setText("");
                                    tableView.doFindAction(findNextOne, findingText,
                                            applyMatchCase, applyRegex, description);
                                }
                            }
                        };
                        next.addActionListener(actionListener);
                        prev.addActionListener(actionListener);
                        text.getActionMap().put(":enter", new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String findingText = text.getText();
                                boolean applyMatchCase = matchCase.isSelected();
                                boolean applyRegex = regex.isSelected();
                                if (tableView != null) {
                                    description.setText("");
                                    tableView.doFindAction(true, findingText,
                                            applyMatchCase, applyRegex, description);
                                }
                            }
                        });
                        text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ":enter");
                        panel.add(hand);
                        panel.add(text);
                        panel.add(next);
                        panel.add(prev);
                        panel.add(matchCase);
                        panel.add(regex);
                        panel.add(description);
                        panel.add(close);
                        dialog.setContentPane(panel);
                        dialog.setSize(panel.getPreferredSize());
                        Point tableLoc = tableView.getParent().getLocationOnScreen();
                        if (tableLoc.x >= 0 && tableLoc.y > 0) {
                            dialog.setLocation(tableLoc);
                        } else {
                            dialog.setLocationRelativeTo(null);
                        }
                        dialog.setVisible(true);
                    } else if (keyCode == KeyEvent.VK_ESCAPE &&
                            (dialog != null && dialog.isVisible() && dialog.isShowing())) {
                        focusableNode = null;
                        dialog.dispose();
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    private void show() {
        tableView = new LogTableView();
        logTableModel = (LogTableModel) tableView.getModel();
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(new EmptyBorder(5, 10, 5, 10));
        toolbar.add(new JLabel("<html><b>Message Filter Example:</b> <br/>" +
                "<font color='red'>[pid:1024 tid:64 tag:Activ ]<br/>" +
                "[app:robo thread:main text:Con]</font></html>"),
                BorderLayout.WEST);
        toolbar.add(tableView.getMessageFilter(), BorderLayout.CENTER);
        JPanel selectors = new JPanel();
        BoxLayout boxLayout = new BoxLayout(selectors, BoxLayout.LINE_AXIS);
        selectors.setLayout(boxLayout);
        selectors.add(tableView.getLogicalSelector());
        selectors.add(tableView.getLevelSelector());
        selectors.add(new JLabel("<html><b>Go to Line:</b> </html>"));
        selectors.add(tableView.getGotoLine());
        toolbar.add(selectors, BorderLayout.EAST);
        JPanel jPanel = new JPanel(new BorderLayout());
        JScrollPane jScrollPane = new JScrollPane(tableView);
        jPanel.add(jScrollPane, BorderLayout.CENTER);
        jPanel.add(toolbar, BorderLayout.NORTH);
        jFrame = new JFrame("LogcatFileReader[loading...]");
        jFrame.setContentPane(jPanel);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setResizable(true);
        jFrame.setSize(1000, 500);
        jFrame.setMinimumSize(new Dimension(600, 200));
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
        registerFindShotcutAction();
        synchronized(this) {
            notifyAll();
        }
    }

    public void main(final File logFile, final LogCatMessageParser parser) {
        new Thread("Fetch-Data-Thread") {
            @Override
            public void run() {
                List<LogCatMessage> messageList;
                if (logFile == null) {
                    messageList = parser.processLogLines(MESSAGES);
                } else {
                    try {
                        messageList = parser.processLogLines(logFile);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Failed to load the log file");
                        e.printStackTrace();
                        messageList = parser.processLogLines(MESSAGES);
                    }
                }
                synchronized(Reader.this) {
                    try {
                        while (logTableModel == null) {
                            Reader.this.wait(1000L);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (logTableModel != null) {
                    logTableModel.setDataSource(messageList);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            jFrame.setTitle("LogcatFileReader");
                        }
                    });
                }
            }
        }.start();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                show();
            }
        });
    }

    private static File logFile;
    private static LogCatMessageParser parser;

    public static void main(final String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "No optional system style UI skin");
                    e.printStackTrace();
                }

                parser = new LogCatMessageParser(); //default: -v long
                if (args.length > 0) {
                    logFile = new File(args[0]);
                    if (!logFile.exists() || !logFile.isFile() || !logFile.canRead()) {
                        logFile = null;
                    }
                    if (args.length == 2) {
                        switch (args[1]) {
                            case "threadtime":
                                parser = new LogCatMessageParser2();
                                break;
                            case "javacustom":
                                parser = new LogCatMessageParser3();
                                break;
                        }
                    }
                } else {
                    final JDialog dialog = new JDialog((JFrame)null, true);
                    final JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDragEnabled(true);
                    fileChooser.setFileHidingEnabled(false);
                    fileChooser.setMultiSelectionEnabled(false);
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().endsWith(".log");
                        }
                        @Override
                        public String getDescription() {
                            return "*.log";
                        }
                    });
                    fileChooser.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                                logFile = fileChooser.getSelectedFile();
                            }
                            dialog.dispose();
                        }
                    });
                    final JComboBox<String> formatExample = new JComboBox<>(new String[]{
                            "[ 04-25 21:53:28.440  1190: 1190 I/MyTag ]\n"
                                    + "this is message body",
                            "04-25 21:53:28.440  1190  1190 I MyTag: this is message body",
                            "[2016-04-25 21:53:28.440][main][INFO ]MyTag - this is message body"
                    });
                    formatExample.setRenderer(new ListCellRenderer<String>() {
                        private JTextArea item = new JTextArea();
                        @Override
                        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                         int index, boolean isSelected, boolean cellHasFocus) {
                            item.setText(value);
                            if (isSelected) {
                                item.setBackground(list.getSelectionBackground());
                                item.setForeground(list.getSelectionForeground());
                            } else {
                                item.setBackground(list.getBackground());
                                item.setForeground(list.getForeground());
                            }
                            return item;
                        }
                    });
                    formatExample.setEditable(false);
                    final String labelPrefix = "  log file text format:";
                    final JLabel formatDescription = new JLabel(labelPrefix + " long ");
                    formatExample.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                switch (formatExample.getSelectedIndex()) {
                                    case 0:
                                        formatDescription.setText(labelPrefix + " long ");
                                        break;
                                    case 1:
                                        formatDescription.setText(labelPrefix + " threadtime ");
                                        break;
                                    case 2:
                                        formatDescription.setText(labelPrefix + " javacustom ");
                                        break;
                                    default:
                                        formatDescription.setText(labelPrefix + " long ");
                                        break;
                                }
                            }
                        }
                    });
                    JPanel bottom = new JPanel(new BorderLayout());
                    bottom.add(formatDescription, BorderLayout.WEST);
                    bottom.add(formatExample, BorderLayout.CENTER);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JLabel("  which log text file would you want to open: "), BorderLayout.NORTH);
                    panel.add(fileChooser, BorderLayout.CENTER);
                    panel.add(bottom, BorderLayout.SOUTH);
                    dialog.setContentPane(panel);
                    dialog.setTitle("Open File Dialog");
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setAlwaysOnTop(true);
                    dialog.setSize(panel.getPreferredSize());
                    dialog.setMinimumSize(new Dimension(600, 200));
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true);
                    switch (formatExample.getSelectedIndex()) {
                        case 1:
                            parser = new LogCatMessageParser2();
                            break;
                        case 2:
                            parser = new LogCatMessageParser3();
                            break;
                    }
                }
                new Reader().main(logFile, parser);
            }
        });
    }

}

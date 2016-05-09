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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.johnsoft.logcat.LogCatMessage;
import com.johnsoft.logcat.LogCatMessageParser;
import com.johnsoft.logcat.LogCatMessageParser2;
import com.johnsoft.logcat.LogCatMessageParser3;

/**
 * @author John Kenrinus Lee
 * @version 2016-04-25
 */
public class Reader {
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

    private LogTableModel logTableModel;
    private JFrame jFrame;

    private void show() {
        LogTableView tableView = new LogTableView();
        logTableModel = (LogTableModel) tableView.getModel();
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.add(new JLabel(" Message Filter[e.g. : \"pid:1024 tag:Activ text:Con\"]: "), BorderLayout
                .WEST);
        toolbar.add(tableView.getMessageFilter(), BorderLayout.CENTER);
        toolbar.add(tableView.getLevelSelector(), BorderLayout.EAST);
        JPanel jPanel = new JPanel(new BorderLayout());
        JScrollPane jScrollPane = new JScrollPane(tableView);
        jPanel.add(jScrollPane, BorderLayout.CENTER);
        jPanel.add(toolbar, BorderLayout.NORTH);
        jFrame = new JFrame("LogcatFileReader[loading...]");
        jFrame.setContentPane(jPanel);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setResizable(true);
        jFrame.setSize(1000, 400);
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
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
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "No optional system style UI skin");
                    e.printStackTrace();
                }
                show();
            }
        });
    }

    public static void main(String[] args) {
        File logFile = null;
        LogCatMessageParser parser = new LogCatMessageParser(); //default: -v long
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
        }
        new Reader().main(logFile, parser);
    }
}

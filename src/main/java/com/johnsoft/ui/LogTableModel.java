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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.johnsoft.logcat.LogCatFilter;
import com.johnsoft.logcat.LogCatMessage;

/**
 * @author John Kenrinus Lee
 * @version 2016-04-25
 */
public class LogTableModel extends AbstractTableModel {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;
    private final String[] columnHeaders = new String[] {
            "Level", "Time", "PID", "TID", "Application", "Thread", "Tag", "Text"
    };
    private List<LogCatMessage> modelList = new ArrayList<>();
    private List<Integer> viewList = new ArrayList<>();

    /** should call this method only once */
    public final void setDataSource(List<LogCatMessage> list) {
        this.modelList = Collections.unmodifiableList(list);
        final ArrayList<Integer> indexList = new ArrayList<>(modelList.size());
        for (int i = 0; i < modelList.size(); ++i) {
            indexList.add(i);
        }
        synchronized(LogTableModel.this) {
            this.viewList = indexList;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireTableDataChanged();
            }
        });
    }

    public final void setRowFilter(final List<LogCatFilter> list) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("call this method from event dispatch thread");
        }
        try {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        scheduledFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Integer> indexList = new ArrayList<>();
                for (int i = 0; i < modelList.size(); ++i) {
                    LogCatMessage message = modelList.get(i);
                    boolean match = true;
                    for (LogCatFilter f : list) {
                        if (!f.matches(message)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        indexList.add(i);
                    }
                }
                synchronized(LogTableModel.this) {
                    viewList = indexList;
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        fireTableDataChanged();
                    }
                });
            }
        }, 800L, TimeUnit.MILLISECONDS);
    }

    public final String getLogLevel(int row) {
        final LogCatMessage message;
        synchronized(LogTableModel.this) {
            message = modelList.get(viewList.get(row));
        }
        return String.valueOf(message.getLogLevel().getPriorityLetter());
    }

    @Override
    public String getColumnName(int column) {
        return columnHeaders[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public int getRowCount() {
        return viewList.size();
    }

    @Override
    public int getColumnCount() {
        return columnHeaders.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final LogCatMessage message;
        synchronized(LogTableModel.this) {
            if (viewList == null || rowIndex >= viewList.size()) {
                return null;
            }
            message = modelList.get(viewList.get(rowIndex));
        }
        if (message.isOnlyBody() && columnIndex != 7) {
            return "";
        }
        switch (columnIndex) {
            case 0:
                return message.getLogLevel().getPriorityLetter();
            case 1:
                return message.getTime();
            case 2:
                return message.getPid();
            case 3:
                return message.getTid();
            case 4:
                return message.getAppName();
            case 5:
                return message.getThreadName();
            case 6:
                return message.getTag();
            case 7:
                return message.getMessage();
            default:
                return null;
        }
    }
}

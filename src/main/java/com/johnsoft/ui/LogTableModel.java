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
import com.johnsoft.logcat.LogCatMessage;
import com.johnsoft.logcat.LogicalPredicate;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author John Kenrinus Lee
 * @version 2016-04-25
 */
public final class LogTableModel extends AbstractTableModel implements LogTableView.CommonModel {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    /** should not update it except from setDataSource(List) */
    private List<LogCatMessage> modelList = new ArrayList<>();
    /** should not update it except from setDataSource(List) */
    private int modelSize;

    private List<Integer> viewList = new ArrayList<>();

    /** should call this method only once */
    public final void setDataSource(List<LogCatMessage> list) {
        this.modelList = Collections.unmodifiableList(list);
        this.modelSize = modelList.size();
        final ArrayList<Integer> indexList = new ArrayList<>(modelSize);
        for (int i = 0; i < modelSize; ++i) {
            indexList.add(i);
        }
        synchronized (LogTableModel.this) {
            this.viewList = indexList;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireTableDataChanged();
            }
        });
    }

    public final void setRowFilter(final List<LogCatFilter> list, final LogicalPredicate predicate) {
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
                for (int i = 0; i < modelSize; ++i) {
                    LogCatMessage message = modelList.get(i);
                    boolean match;
                    if (predicate == LogicalPredicate.AND) {
                        match = true;
                        for (LogCatFilter f : list) {
                            if (!f.matches(message)) {
                                match = false;
                                break;
                            }
                        }
                    } else if (predicate == LogicalPredicate.OR) {
                        match = false;
                        for (LogCatFilter f : list) {
                            if (f.matches(message)) {
                                match = true;
                                break;
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown LogicalPredicate");
                    }
                    if (match) {
                        indexList.add(i);
                    }
                }
                synchronized (LogTableModel.this) {
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

    public final int getModelRowIndex(int row) {
        synchronized (LogTableModel.this) {
            return viewList == null ? -1 : viewList.get(row);
        }
    }

    @Override
    public final String getLogLevel(int row) {
        final LogCatMessage message;
        message = modelList.get(getModelRowIndex(row));
        return String.valueOf(message.getLogLevel().getPriorityLetter());
    }

    @Override
    public String getColumnName(int column) {
        return "";
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
        synchronized (LogTableModel.this) {
            return viewList == null ? -1 : viewList.size();
        }
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= getRowCount()) {
            return "";
        }
        return valueAt(modelList.get(getModelRowIndex(rowIndex)));
    }

    public final int doFind(int from, boolean findNextOne, String findingText, boolean matchCase, boolean regex) {
        int size = getRowCount();
        if (size <= 0) {
            return -1;
        }
        if (findingText == null || findingText.trim().isEmpty()) {
            return -1;
        }
        if (!matchCase) {
            findingText = findingText.toLowerCase();
        }
        int start = 0;
        int result = doFind(from, findNextOne, findingText, matchCase, regex, size, start);
        if (result >= 0) {
            return result;
        }
        if (findNextOne) {
            size = from;
            from = 0;
        } else {
            start = from;
            from = size - 1;
        }
        return doFind(from, findNextOne, findingText, matchCase, regex, size, start);
    }

    private int doFind(int from, boolean findNextOne, String findingText,
                        boolean matchCase, boolean regex, int size, int start) {
        final Pattern pattern;
        if (regex) {
            pattern = Pattern.compile(findingText);
        } else {
            pattern = null;
        }
        while (true) {
            if (findNextOne) {
                if (from >= size) {
                    break;
                }
            } else {
                if (from < start) {
                    break;
                }
            }
            final int idx = getModelRowIndex(from);
            if (idx < 0) {
                return -1;
            }
            final LogCatMessage message = modelList.get(idx);
            if (message == null) {
                continue;
            }
            String msg = valueAt(message);
            if (msg == null || msg.trim().isEmpty()) {
                continue;
            }
            if (!matchCase) {
                msg = msg.toLowerCase();
            }
            boolean success;
            if (pattern != null) {
                success = pattern.matcher(msg).find();
            } else {
                success = msg.contains(findingText);
            }
            if (success) {
                return from;
            }
            if (findNextOne) {
                ++from;
            } else {
                --from;
            }
        }
        return -1;
    }

    public final SubLogTableModel subView(int modelRow, int halfRegion) {
        final int rowCount = modelSize;
        if (modelRow >= rowCount || modelRow < 0) {
            return null;
        }
        int up = modelRow - halfRegion;
        int down = modelRow + halfRegion;
        if (up < 0) {
            up = 0;
        }
        if (down >= rowCount) {
            down = rowCount - 1;
        }
        if (down < up) {
            return null;
        }
        ArrayList<LogCatMessage> subList = new ArrayList<>(down - up + 2);
        for (int i = up; i <= down; ++i) {
            subList.add(modelList.get(i));
        }
        return new SubLogTableModel(subList, (modelRow - up));
    }

    public static final class SubLogTableModel extends AbstractTableModel implements LogTableView.CommonModel {
        private final List<LogCatMessage> modelList;
        private final int modelSize;
        private final int from;

        public SubLogTableModel(List<LogCatMessage> list, int from) {
            this.modelList = Collections.unmodifiableList(list);
            this.modelSize = modelList.size();
            this.from = from;
        }

        public int getFrom() {
            return from;
        }

        @Override
        public final String getLogLevel(int row) {
            final LogCatMessage message;
            message = modelList.get(row);
            return String.valueOf(message.getLogLevel().getPriorityLetter());
        }

        @Override
        public String getColumnName(int column) {
            return "";
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
            return modelSize;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= getRowCount()) {
                return "";
            }
            return LogTableModel.valueAt(modelList.get(rowIndex));
        }
    }

    private static String valueAt(LogCatMessage message) {
        if (message == null) {
            return "";
        }
        return message.getCommonHeader() + message.getMessage();
    }
}

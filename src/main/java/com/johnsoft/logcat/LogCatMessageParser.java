/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.johnsoft.logcat;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class to parse raw output to {@link LogCatMessage} objects.
 */
public abstract class LogCatMessageParser {
    public enum ParsePolicy {
        COMMON_HEADER_EACH, COMMON_HEADER_FIRST_BLANKING, COMMON_HEADER_FIRST_PLAIN
    }

    public static final ParsePolicy getParsePolicy() {
        return ParsePolicy.COMMON_HEADER_FIRST_BLANKING;
    }

    protected static final void followLastMessage(String line, List<LogCatMessage> messages) {
        if (!messages.isEmpty()) {
            final LogCatMessage m = messages.get(messages.size() - 1);
            final String commonHeader;
            if (ParsePolicy.COMMON_HEADER_EACH.equals(getParsePolicy())) {
                commonHeader = m.getCommonHeader();
            } else if (ParsePolicy.COMMON_HEADER_FIRST_BLANKING.equals(getParsePolicy())) {
                commonHeader = getBlank(m.getCommonHeader().length());
            } else {
                commonHeader = "";
            }
            messages.add(new LogCatMessage(m.getLogLevel(),
                    m.getPid(),
                    m.getTid(),
                    m.getAppName(),
                    m.getThreadName(),
                    m.getTag(),
                    m.getTime(),
                    line,
                    commonHeader));
        }
    }

    protected static final String getBlank(int length) {
        String blank = "";
        for (int i = 0; i < length; ++i) {
            blank += ' ';
        }
        return blank;
    }

    public final List<LogCatMessage> processLogLines(String[] lines) {
        final List<LogCatMessage> messages = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            processLogLine(line, messages);
        }
        return messages;
    }

    public final List<LogCatMessage> processLogLines(File logFile)
                                                throws IOException {
        final List<LogCatMessage> messages = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                processLogLine(line, messages);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return messages;
    }

    protected abstract void processLogLine(String line, List<LogCatMessage> messages);
}

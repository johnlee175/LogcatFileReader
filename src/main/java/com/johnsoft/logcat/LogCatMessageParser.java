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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse raw output of {@code adb logcat -v long} to {@link LogCatMessage} objects.
 */
public class LogCatMessageParser {
    private LogLevel mCurLogLevel = LogLevel.WARN;
    private String mCurPid = "?";
    private String mCurTid = "?";
    private String mCurTag = "?";
    private String mCurTime = "?:??";

    /**
     * This pattern is meant to parse the first line of a log message with the option
     * 'logcat -v long'. The first line represents the date, tag, severity, etc.. while the
     * following lines are the message (can be several lines).<br>
     * This first line looks something like:<br>
     * {@code "[ 00-00 00:00:00.000 <pid>:0x<???> <severity>/<tag>]"}
     * <br>
     * Note: severity is one of V, D, I, W, E, A? or F. However, there doesn't seem to be
     *       a way to actually generate an A (assert) message. Log.wtf is supposed to generate
     *       a message with severity A, however it generates the undocumented F level. In
     *       such a case, the parser will change the level from F to A.<br>
     * Note: the fraction of second value can have any number of digit.<br>
     * Note: the tag should be trimmed as it may have spaces at the end.
     */
    private static final Pattern sLogHeaderPattern = Pattern.compile(
            "^\\[\\s(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)"
          + "\\s+(\\d*):\\s*(\\S+)\\s([VDIWEAF])/(.*)\\]$");

    /**
     * Parse a list of strings into {@link LogCatMessage} objects. This method
     * maintains state from previous calls regarding the last seen header of
     * logcat messages.
     * @param lines list of raw strings obtained from logcat -v long
     * @return list of LogMessage objects parsed from the input
     */
    public List<LogCatMessage> processLogLines(String[] lines) {
        final List<LogCatMessage> messages = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            processLogLine(line, messages);
        }
        return messages;
    }

    public List<LogCatMessage> processLogLines(File logFile)
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

    protected void processLogLine(String line, List<LogCatMessage> messages) {
        final Matcher matcher = sLogHeaderPattern.matcher(line);
        if (matcher.matches()) {
            mCurTime = matcher.group(1);
            mCurPid = matcher.group(2);
            mCurTid = matcher.group(3);
            mCurLogLevel = LogLevel.getByLetterString(matcher.group(4));
            mCurTag = matcher.group(5).trim();
            /* LogLevel doesn't support messages with severity "F". Log.wtf() is supposed
             * to generate "A", but generates "F". */
            if (mCurLogLevel == null && matcher.group(4).equals("F")) {
                mCurLogLevel = LogLevel.ASSERT;
            }
        } else {
            boolean flag = false;
            for (String txtMsg : splitTextWithFixLength(line, DEFAULT_LIMIT)) {
                String pkgName = "";
                String threadName = "";
                messages.add(new LogCatMessage(mCurLogLevel, mCurPid, mCurTid,
                        pkgName, threadName, mCurTag, mCurTime, txtMsg, flag));
                if (!flag) {
                    flag = true;
                }
            }
        }
    }

    public static final int DEFAULT_LIMIT = 160;

    public static final List<String> splitTextWithFixLength(String text, int limit) {
        List<String> strings = new ArrayList<>();
        int offset = 0, pos = 0;
        while (text.length() > (pos = offset + limit)) {
            strings.add(text.substring(offset, pos));
            offset = pos;
        }
        if ((text.length() - offset) > 0) {
            strings.add(text.substring(offset, text.length()));
        }
        return strings;
    }
}

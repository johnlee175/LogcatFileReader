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
package com.johnsoft.logcat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse raw output of {@code adb logcat -v threadtime} to {@link LogCatMessage} objects.
 * @author John Kenrinus Lee
 * @version 2016-04-26
 */
public class LogCatMessageParser2 extends LogCatMessageParser {
    //04-25 19:04:38.041  1190  1190 I MyTag: this is message body
    private static final Pattern sLogPattern = Pattern.compile(
            "^((\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)"
                    + "\\s+(\\d*)\\s*(\\S+)\\s([VDIWEAF])\\s(.*?):\\s)(.*)");

    @Override
    protected void processLogLine(String line, List<LogCatMessage> messages) {
        final Matcher matcher = sLogPattern.matcher(line);
        if (matcher.matches()) {
            /* LogLevel doesn't support messages with severity "F". Log.wtf() is supposed
             * to generate "A", but generates "F". */
            LogLevel currLogLevel = LogLevel.getByLetterString(matcher.group(5));
            if (currLogLevel == null && matcher.group(5).equals("F")) {
                currLogLevel = LogLevel.ASSERT;
            }
            String pkgName = "";
            String threadName = "";
            messages.add(new LogCatMessage(currLogLevel,
                    matcher.group(3)/*currPid*/,
                    matcher.group(4)/*currTid*/,
                    pkgName,
                    threadName,
                    matcher.group(6).trim()/*currTag*/,
                    matcher.group(2)/*currTime*/,
                    matcher.group(7)/*currMsg*/,
                    matcher.group(1)/*commonHeader*/));
        } else {
            followLastMessage(line, messages);
        }
    }
}

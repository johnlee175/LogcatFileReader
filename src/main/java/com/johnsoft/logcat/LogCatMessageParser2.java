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
    private static final Pattern sLogHeaderPattern = Pattern.compile(
            "^(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)"
                    + "\\s+(\\d*)\\s*(\\S+)\\s([VDIWEAF])\\s(\\w+):\\s(.*)");

    @Override
    protected void processLogLine(String line, List<LogCatMessage> messages) {
        final Matcher matcher = sLogHeaderPattern.matcher(line);
        if (matcher.matches()) {
            /* LogLevel doesn't support messages with severity "F". Log.wtf() is supposed
             * to generate "A", but generates "F". */
            LogLevel currLogLevel = LogLevel.getByLetterString(matcher.group(4));
            if (currLogLevel == null && matcher.group(4).equals("F")) {
                currLogLevel = LogLevel.ASSERT;
            }
            LogCatMessage m = new LogCatMessage(currLogLevel,
                    matcher.group(2)/*currPid*/,
                    matcher.group(3)/*currTid*/,
                    ""/*pkgName*/,
                    ""/*threadName*/,
                    matcher.group(5).trim()/*currTag*/,
                    matcher.group(1)/*currTime*/,
                    matcher.group(6)/*currMsg*/,
                    false/*onlyBody*/);
            messages.add(m);
        }
    }
}

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
 * Class to parse raw output of {@code java.util.logging.Logger.getLogger} to {@link LogCatMessage} objects.
 * @author John Kenrinus Lee
 * @version 2016-04-26
 */
public class LogCatMessageParser3 extends LogCatMessageParser {
    //PatternLayout: [%d{yyyy-MM-dd HH:mm:ss.SSS}][%thread][%-5level]%logger{50} - %msg%n
    //Example: [2016-03-15 13:35:05.503][main][WARN ]MyTag - this is message body
    private static final Pattern sLogHeaderPattern = Pattern.compile(
            "^\\[(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)\\]"
                    + "\\[(\\w+)\\]\\[(\\w{4}.)\\](\\w+)\\s-\\s(.*)");

    @Override
    protected void processLogLine(String line, List<LogCatMessage> messages) {
        final Matcher matcher = sLogHeaderPattern.matcher(line);
        if (matcher.matches()) {
            final LogLevel currLogLevel;
            switch (matcher.group(3).trim()) {
                case "TRACE":
                    currLogLevel = LogLevel.VERBOSE;
                    break;
                case "DEBUG":
                    currLogLevel = LogLevel.DEBUG;
                    break;
                case "INFO":
                    currLogLevel = LogLevel.INFO;
                    break;
                case "WARN":
                    currLogLevel = LogLevel.WARN;
                    break;
                case "ERROR":
                    currLogLevel = LogLevel.ERROR;
                    break;
                default:
                    currLogLevel = LogLevel.ASSERT;
                    break;
            }
            boolean flag = false;
            for (String txtMsg : splitTextWithFixLength(matcher.group(5), DEFAULT_LIMIT)) {
                messages.add(new LogCatMessage(currLogLevel,
                        ""/*currPid*/,
                        ""/*currTid*/,
                        ""/*pkgName*/,
                        matcher.group(2)/*threadName*/,
                        matcher.group(4)/*currTag*/,
                        matcher.group(1)/*currTime*/,
                        txtMsg/*currMsg*/,
                        flag/*onlyBody*/));
                if (!flag) {
                    flag = true;
                }
            }
        } else {
            final LogCatMessage m = messages.get(messages.size() - 1);
            messages.add(new LogCatMessage(m.getLogLevel(),
                    m.getPid(),
                    m.getTid(),
                    m.getAppName(),
                    m.getThreadName(),
                    m.getTag(),
                    m.getTime(),
                    line,
                    false));
        }
    }
}

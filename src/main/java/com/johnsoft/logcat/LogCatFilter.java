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

/**
 * A Filter for logcat messages. A filter can be constructed to match
 * different fields of a logcat message. It can then be queried to see if
 * a message matches the filter's settings.
 */
public interface LogCatFilter {
    LogCatFilter NO_FILTER = new LogCatFilter() {
        @Override
        public String getName() {
            return "No-Filter";
        }

        @Override
        public LogLevel getLogLevel() {
            return LogLevel.VERBOSE;
        }

        @Override
        public boolean matches(LogCatMessage m) {
            return true;
        }
    };

    String getName();

    LogLevel getLogLevel();

    /**
     * Check whether a given message will make it through this filter.
     *
     * @param m message to check
     *
     * @return true if the message matches the filter's conditions.
     */
    boolean matches(LogCatMessage m);
}

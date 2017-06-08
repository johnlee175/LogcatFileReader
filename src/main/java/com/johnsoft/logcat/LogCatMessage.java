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
 * Model a single log message output from {@code logcat -v long}.
 * A logcat message has a {@link LogLevel}, the pid (process id) of the process
 * generating the message, the time at which the message was generated, and
 * the tag and message itself.
 */
public final class LogCatMessage {
    private final long mId;
    private final LogLevel mLogLevel;
    private final String mPid;
    private final String mTid;
    private final String mAppName;
    private final String mThreadName;
    private final String mTag;
    private final String mTime;
    private final String mMessage;
    private final String mCommonHeader;

    /**
     * Construct an immutable log message object.
     */
    public LogCatMessage(long id, LogLevel logLevel, String pid, String tid,
                         String appName, String threadName, String tag,
                         String time, String msg, String commonHeader) {
        mId = id;
        mLogLevel = logLevel;
        mPid = pid;
        mAppName = appName;
        mThreadName = threadName;
        mTag = tag;
        mTime = time;
        mMessage = msg;
        mCommonHeader = commonHeader;

        try {
            // Thread id's may be in hex on some platforms.
            // Decode and store them in radix 10.
            tid = Long.toString(Long.decode(tid.trim()));
        } catch (NumberFormatException e) {
            tid = "";
        }
        mTid = tid;
    }

    public long getId() {
        return mId;
    }

    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    public String getPid() {
        return mPid;
    }

    public String getTid() {
        return mTid;
    }

    public String getAppName() {
        return mAppName;
    }

    public String getThreadName() {
        return mThreadName;
    }

    public String getTag() {
        return mTag;
    }

    public String getTime() {
        return mTime;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getCommonHeader() {
        return mCommonHeader;
    }

    @Override
    public String toString() {
        return "LogCatMessage{" +
                "mId=" + mId +
                ", mLogLevel=" + mLogLevel +
                ", mPid='" + mPid + '\'' +
                ", mTid='" + mTid + '\'' +
                ", mAppName='" + mAppName + '\'' +
                ", mThreadName='" + mThreadName + '\'' +
                ", mTag='" + mTag + '\'' +
                ", mTime='" + mTime + '\'' +
                ", mMessage='" + mMessage + '\'' +
                ", mCommonHeader='" + mCommonHeader + '\'' +
                '}';
    }
}

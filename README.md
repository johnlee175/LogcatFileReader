a java application for show log file generate from logcat

Usage:

if the log file from "adb logcat -v long":
java -jar dist/LogcatFileReader-xxx.jar YOUR_LOG_FILE_PATH

if the log file from "adb logcat -v threadtime":
java -jar dist/LogcatFileReader-xxx.jar YOUR_LOG_FILE_PATH threadtime

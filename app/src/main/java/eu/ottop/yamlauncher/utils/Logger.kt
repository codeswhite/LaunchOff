package eu.ottop.yamlauncher.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Centralized logging utility for YAM Launcher.
 * Provides file-based logging with automatic log rotation and export functionality.
 */
class Logger private constructor(private val context: Context) {

    companion object {
        private const val TAG = "YAMLogger"
        private const val LOG_FILE_NAME = "yam_launcher.log"
        private const val MAX_LOG_FILE_SIZE = 1024 * 1024 * 5L // 5MB
        private const val MAX_LOG_ENTRIES = 5000

        @Volatile
        private var instance: Logger? = null

        fun getInstance(context: Context): Logger {
            return instance ?: synchronized(this) {
                instance ?: Logger(context.applicationContext).also { instance = it }
            }
        }
    }

    private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logEntryCount = 0

    private val logFile: File by lazy {
        File(context.filesDir, LOG_FILE_NAME)
    }

    init {
        initializeLogFile()
    }

    private fun initializeLogFile() {
        if (!logFile.exists()) {
            logFile.createNewFile()
            writeHeader()
        }
    }

    private fun writeHeader() {
        val header = buildString {
            appendLine("========================================")
            appendLine("YAM Launcher Log File")
            appendLine("Started: ${dateFormat.format(Date())}")
            appendLine("App Version: ${getAppVersion()}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("========================================")
            appendLine()
        }
        writeToFile(header)
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Logs a debug message.
     */
    fun d(tag: String, message: String) {
        log("DEBUG", tag, message)
        Log.d(tag, message)
    }

    /**
     * Logs an info message.
     */
    fun i(tag: String, message: String) {
        log("INFO", tag, message)
        Log.i(tag, message)
    }

    /**
     * Logs a warning message.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
        Log.w(tag, message, throwable)
    }

    /**
     * Logs an error message.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
        Log.e(tag, message, throwable)
    }

    /**
     * Logs an exception with its stack trace.
     */
    fun exception(tag: String, message: String, throwable: Throwable) {
        val stackTrace = Log.getStackTraceString(throwable)
        val fullMessage = "$message\n${throwable.javaClass.name}: ${throwable.message}\n$stackTrace"
        log("EXCEPTION", tag, fullMessage)
        Log.e(tag, message, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        logExecutor.execute {
            try {
                if (shouldRotate()) {
                    rotateLogFile()
                }

                val timestamp = dateFormat.format(Date())
                val logLine = if (throwable != null) {
                    val stackTrace = Log.getStackTraceString(throwable)
                    "[$timestamp] $level/$tag: $message\n${stackTrace}\n"
                } else {
                    "[$timestamp] $level/$tag: $message\n"
                }

                writeToFile(logLine)
                logEntryCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log entry", e)
            }
        }
    }

    private fun shouldRotate(): Boolean {
        return logFile.length() > MAX_LOG_FILE_SIZE || logEntryCount > MAX_LOG_ENTRIES
    }

    private fun rotateLogFile() {
        try {
            // Create backup of current log
            val backupFile = File(context.filesDir, "$LOG_FILE_NAME.old")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)

            // Create new log file with header
            logFile.createNewFile()
            logEntryCount = 0
            writeHeader()

            // Delete old backup if it exists (keep only 1 backup)
            val oldBackup = File(context.filesDir, "$LOG_FILE_NAME.old.old")
            if (oldBackup.exists()) {
                oldBackup.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }

    private fun writeToFile(content: String) {
        FileWriter(logFile, true).use { writer ->
            writer.append(content)
        }
    }

    /**
     * Gets the log file for sharing/exporting.
     */
    fun getLogFileForExport(): File = logFile

    /**
     * Gets the current log content as a String.
     */
    fun getLogContent(): String {
        return try {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log file", e)
            ""
        }
    }

    /**
     * Clears the log file.
     */
    fun clearLogs() {
        logExecutor.execute {
            try {
                logFile.delete()
                logFile.createNewFile()
                logEntryCount = 0
                writeHeader()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear log file", e)
            }
        }
    }

    /**
     * Gets the current log file size in bytes.
     */
    fun getLogFileSize(): Long = logFile.length()

    /**
     * Shuts down the logger executor service.
     * Call this when the app is terminating.
     */
    fun shutdown() {
        logExecutor.shutdown()
    }
}

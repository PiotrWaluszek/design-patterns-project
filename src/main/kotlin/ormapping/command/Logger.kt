package ormapping.command

import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe singleton logger that supports multiple log destinations.
 * Each log destination is uniquely identified by its filename and maintains its own FileWriter.
 */
class MultiDestinationLogger private constructor() {
    private val logDestinations = ConcurrentHashMap<String, FileWriter>()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    companion object {
        @Volatile
        private var instance: MultiDestinationLogger? = null

        fun getInstance(): MultiDestinationLogger {
            return instance ?: synchronized(this) {
                instance ?: MultiDestinationLogger().also { instance = it }
            }
        }
    }

    /**
     * Initializes a new log destination with the specified filename.
     * If the destination already exists, it will be reused.
     *
     * @param filename The name of the log file
     * @throws IllegalArgumentException if the filename is invalid
     */
    fun initializeLogDestination(filename: String) {
        try {
            if (!logDestinations.containsKey(filename)) {
                val file = File(filename)
                file.parentFile?.mkdirs()
                logDestinations[filename] = FileWriter(file, true)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to initialize log destination: $filename", e)
        }
    }

    /**
     * Logs a message to the specified destination file.
     *
     * @param filename The destination log file
     * @param message The message to log
     * @throws IllegalStateException if the log destination hasn't been initialized
     */
    fun log(filename: String, message: String) {
        val writer = logDestinations[filename] ?: throw IllegalStateException(
            "Log destination '$filename' not initialized. Call initializeLogDestination() first."
        )

        synchronized(writer) {
            try {
                val timestamp = LocalDateTime.now().format(dateFormatter)
                writer.write("[$timestamp] $message\n")
                writer.flush()
            } catch (e: Exception) {
                System.err.println("Failed to write to log file '$filename': ${e.message}")
            }
        }
    }

    /**
     * Closes all open log destinations.
     * Should be called when the application is shutting down.
     */
    fun closeAll() {
        logDestinations.forEach { (_, writer) ->
            try {
                writer.close()
            } catch (e: Exception) {
            }
        }
        logDestinations.clear()
    }
}

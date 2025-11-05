import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object LogFileUtil {

    private const val LOG_FILE_NAME = "app_log.txt"
    private const val LOGCAT_FILE_NAME = "app_logcat.txt"

    private var logcatThread: Thread? = null

    // Appends a log message with date and time
    fun appendLog(message: String) {
        try {
            val logFile = getLogFile(LOG_FILE_NAME)
            val dateTime = getCurrentDateTime()
            val formattedMessage = "$dateTime - $message"

            FileWriter(logFile, true).use { writer ->
                PrintWriter(writer).use { out ->
                    out.println(formattedMessage)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Log this to the console if writing to the file fails
        }
    }

    // Start capturing Logcat output to `app_logcat.txt`
    fun startLogcatCapture() {
        if (logcatThread != null && logcatThread!!.isAlive) {
            return // Logcat logging is already running
        }

        logcatThread = Thread {
            try {
                // Clear previous logs (optional)
                Runtime.getRuntime().exec("logcat -c")

                // Run logcat command
                val process = Runtime.getRuntime().exec("logcat")
                val reader = InputStreamReader(process.inputStream)
                val buffer = CharArray(1024)
                var bytesRead: Int

                val logcatFile = getLogFile(LOGCAT_FILE_NAME)
                FileWriter(logcatFile, true).use { writer ->
                    while (reader.read(buffer).also { bytesRead = it } > 0) {
                        val logLine = String(buffer, 0, bytesRead)
                        writer.append(logLine)
                        writer.flush()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        logcatThread!!.start()
    }

    // Stop capturing Logcat output
    fun stopLogcatCapture() {
        logcatThread?.interrupt()
        logcatThread = null
    }

    // Get or create the specified log file in DCIM/Camera directory
    private fun getLogFile(fileName: String): File {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        if (!directory.exists()) {
            directory.mkdirs() // Create directory if it doesn't exist
        }

        val logFile = File(directory, fileName)
        if (!logFile.exists()) {
            logFile.createNewFile() // Explicitly create the file if it doesn't exist
        }

        return logFile
    }

    // Get the current date and time as a formatted string
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}

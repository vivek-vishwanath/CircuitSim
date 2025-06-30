package com.ra4king.circuitsim.gui

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonType
import javafx.scene.control.TextArea
import javafx.stage.Modality
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.Volatile

/**
 * @author Roi Atalla
 */
class DebugUtil internal constructor(private val simulatorWindow: CircuitSim) {
    @Volatile
    private var showingError = false

    @JvmOverloads
    fun logException(throwable: Throwable, message: String = "") {
        System.err.println(message)
        throwable.printStackTrace()

        if (simulatorWindow.openWindow) {
            Platform.runLater {
                synchronized(this@DebugUtil) {
                    if (showingError) return@runLater
                    showingError = true
                }
                try {
                    val stream = ByteArrayOutputStream()
                    throwable.printStackTrace(PrintStream(stream))
                    val errorMessage = stream.toString()

                    val alert = Alert(AlertType.ERROR)
                    alert.initOwner(simulatorWindow.stage)
                    alert.initModality(Modality.WINDOW_MODAL)
                    alert.title = "Internal error"
                    alert.headerText = "Internal error: $message"
                    val textArea = TextArea(errorMessage)
                    textArea.minWidth = 600.0
                    textArea.minHeight = 400.0
                    alert.dialogPane.content = textArea

                    alert.buttonTypes.clear()
                    alert.buttonTypes.add(ButtonType("Save and Exit", ButtonData.APPLY))
                    alert.buttonTypes.add(ButtonType("Send Error Report", ButtonData.YES))
                    alert.buttonTypes.add(ButtonType("Cancel", ButtonData.CANCEL_CLOSE))
                    val buttonType = alert.showAndWait()

                    if (buttonType.isPresent) {
                        if (buttonType.get().buttonData == ButtonData.YES) {
                            sendErrorReport("$message\n$errorMessage")
                        } else if (buttonType.get().buttonData == ButtonData.APPLY) {
                            try {
                                simulatorWindow.saveCircuits()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            simulatorWindow.closeWindow()
                        }
                    }
                } finally {
                    showingError = false
                }
            }
        }
    }

    private fun sendErrorReport(message: String) {
        val messageBuilder = StringBuilder()
        for (property in SYSTEM_PROPERTIES) {
            messageBuilder.append(property).append("=").append(System.getProperty(property)).append("\n")
        }

        messageBuilder.append("CircuitSim version=" + CircuitSim.VERSION).append("\n\n")

        val msg = messageBuilder.append(message.replace("\t", "    ").replace("\r", "")).toString()

        Thread {
            try {
                val httpConnection =
                    URL("https://www.roiatalla.com/circuitsimerror").openConnection() as HttpURLConnection
                httpConnection.setRequestMethod("POST")
                httpConnection.setDoInput(true)
                httpConnection.setDoOutput(true)
                val printWriter = PrintWriter(httpConnection.getOutputStream())
                printWriter.write(msg)
                printWriter.flush()

                httpConnection.getInputStream().read()
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }.start()
    }

    companion object {
        private val SYSTEM_PROPERTIES = arrayOf(
            "java.version",
            "java.vendor",
            "java.vm.specification.version",
            "java.vm.specification.vendor",
            "java.vm.specification.name",
            "java.vm.version",
            "java.vm.vendor",
            "java.vm.name",
            "java.specification.version",
            "java.specification.vendor",
            "java.specification.name",
            "os.name",
            "os.arch",
            "os.version",
        )
    }
}

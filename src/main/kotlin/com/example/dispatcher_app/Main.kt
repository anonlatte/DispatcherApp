package com.example.dispatcher_app

import com.jfoenix.controls.JFXTreeTableView
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

class Main : Application() {
    override fun start(primaryStage: Stage?) {
        val pref = Preferences.userNodeForPackage(Main::class.java)
        val savedToken = pref.get("auth_token", "")
        val savedLogin = pref.get("login", "")

        val properties = Properties()
        val input: InputStream
        try {
            input = FileInputStream("gradle.properties")
            properties.load(input)
            SERVER_ADDRESS = properties.getProperty("ServerAddress")
            SERVER_PORT = properties.getProperty("ServerPort").toInt()
            API_VERSION = properties.getProperty("API_VERSION")
            GOOGLE_MAPS_API_KEY = properties.getProperty("GOOGLE_MAPS_API_KEY")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (savedToken.isNotEmpty()) {
            GlobalScope.launch {
                val managedChannel =
                    ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT).usePlaintext().build()
                val blockingStub = taxiServiceGrpc.newBlockingStub(managedChannel)
                val tokenCheckRequest = TokenCheckRequest.newBuilder()
                    .setApi(API_VERSION)
                    .setUserType(2)
                    .setAuthToken(savedToken)
                    .setLogin(savedLogin)
                    .build()
                val tokenCheckResponse: TokenCheckResponse
                try {
                    tokenCheckResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                        .tokenCheck(tokenCheckRequest) // Запрос на создание
                    managedChannel.shutdown()
                    if (tokenCheckResponse.isValidToken) {
                        Platform.runLater {
                            loadFxml("activity_main.fxml", "Диспетчер")
                        }
                    } else {
                        Platform.runLater {
                            loadFxml("activity_sign_in.fxml", "Вход")
                        }
                    }
                } catch (e: StatusRuntimeException) {
                    if (e.status.cause is java.net.ConnectException) {
                        Platform.runLater {
                            val alert = Alert(Alert.AlertType.ERROR)
                            alert.title = "Ошибка"
                            alert.headerText = "Ошибка соединения"
                            alert.showAndWait()
                        }
                    }
                    managedChannel.shutdown()

                    Platform.runLater {
                        loadFxml("activity_sign_in.fxml", "Вход")
                    }
                }
            }
        } else {
            loadFxml("activity_sign_in.fxml", "Вход")
        }
    }

    companion object {
        var SERVER_ADDRESS: String = ""
        var SERVER_PORT: Int = 0
        var API_VERSION: String = ""
        var GOOGLE_MAPS_API_KEY: String = ""

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }

        @JvmStatic
        fun loadFxml(path: String, title: String) {
            try {
                val fxmlLoader = FXMLLoader(Main::class.java.getResource(path))
                val root = fxmlLoader.load<Parent>()
                val primaryStage = Stage()
                primaryStage.scene = Scene(root)
                primaryStage.title = title
                primaryStage.show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun loadFxml(scene: Scene, path: String, title: String) {
            scene.window.hide()
            loadFxml(path, title)
        }

        @JvmStatic
        fun <T : RecursiveTreeObject<T>?> saveTableToFile(table: JFXTreeTableView<T>, fileName: String) {

            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet("Drivers sheet")

            val rowNum = 0
            var row: Row
            //
            row = sheet.createRow(rowNum)

            GlobalScope.launch {
                for (i in 0 until table.columns.size) {
                    row.createCell(i).setCellValue(table.columns[i].textProperty().value)
                }
                for (i in 0 until table.currentItemsCount) {
                    row = sheet.createRow(i + 1)
                    for (j in 0 until table.columns.size) {
                        if (table.columns[j].getCellData(i) != null) {
                            row.createCell(j).setCellValue(table.columns[j].getCellData(i).toString())
                        } else {
                            row.createCell(j).setCellValue("")
                        }
                    }
                }
            }
            val outFile = FileOutputStream("${fileName}_" + SimpleDateFormat("yyy-MM-dd").format(Date()) + ".xls")
            workbook.write(outFile)
            System.out.println("Created file")
            Platform.runLater {
                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = "Создание файла"
                alert.headerText = "Файл был создан"
                alert.showAndWait()
            }
        }
    }
}
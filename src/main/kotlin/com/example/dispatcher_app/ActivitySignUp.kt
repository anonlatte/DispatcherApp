package com.example.dispatcher_app

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ActivitySignUp {
    @FXML
    lateinit var signInLabel: JFXButton
    @FXML
    lateinit var surnameEdit: JFXTextField
    @FXML
    lateinit var nameEdit: JFXTextField
    @FXML
    lateinit var patronymicEdit: JFXTextField
    @FXML
    lateinit var phoneNumberEdit: JFXTextField
    @FXML
    lateinit var emailEdit: JFXTextField
    @FXML
    lateinit var passwordEdit: JFXTextField

    fun signInScene() {
        Main.loadFxml(signInLabel.scene, "activity_sign_in.fxml", "Вход")
    }

    // TODO validation
    fun registerUser() {
        GlobalScope.launch {
            val managedChannel = ManagedChannelBuilder.forAddress(
                Main.SERVER_ADDRESS,
                Main.SERVER_PORT
            ).usePlaintext().build()
            val blockingStub = taxiServiceGrpc.newBlockingStub(managedChannel)
//            val phoneText = countryCodePicker.selectedCountryCode + phoneEditText!!.text
            val dispatcher = Dispatcher.newBuilder()
                .setFirstName(nameEdit.text)
                .setSurname(surnameEdit.text)
                .setPartronymic(patronymicEdit.text)
                .setPhoneNumber(phoneNumberEdit.text)
                .setEmail(emailEdit.text)
                .setPassword(passwordEdit.text)
                .build()
            val createDispatcherRequest = CreateDispatcherRequest.newBuilder()
                .setApi(Main.API_VERSION)
                .setDispatcher(dispatcher)
                .build()
            val createDispatcherResponse: CreateDispatcherResponse
            try {
                createDispatcherResponse = blockingStub.createDispatcher(createDispatcherRequest) // Запрос на создание
                managedChannel.shutdown()
                Platform.runLater {
                    signInScene()
                }
            } catch (e: StatusRuntimeException) {
                if (e.status.cause is java.net.ConnectException) {
                    Platform.runLater {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Ошибка"
                        alert.headerText = "Ошибка соединения"
                        alert.showAndWait()
                    }
                } else if (e.status.code == Status.Code.NOT_FOUND || e.status.code == Status.Code.PERMISSION_DENIED) {
                    Platform.runLater {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Ошибка"
                        alert.headerText = "Неверные данные"
                        alert.showAndWait()
                    }
                } else if (e.status.code == Status.Code.UNKNOWN) {
                    Platform.runLater {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Ошибка"
                        alert.headerText = "Ошибка сервера"
                        alert.showAndWait()
                    }
                }
                e.printStackTrace()
                managedChannel.shutdown()
            }
        }
    }
}
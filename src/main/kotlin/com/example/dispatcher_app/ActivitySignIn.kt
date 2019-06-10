package com.example.dispatcher_app

import com.example.dispatcher_app.taxiServiceGrpc.newBlockingStub
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.Label
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

class ActivitySignIn : Initializable {

    @FXML
    lateinit var changeToRegisterButton: JFXButton
    @FXML
    lateinit var phoneNumberEdit: JFXTextField
    @FXML
    lateinit var passwordEdit: JFXTextField
    @FXML
    lateinit var passwordLabel: Label
    private lateinit var pref: Preferences

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pref = Preferences.userNodeForPackage(Main::class.java)
    }

    fun registerScene() {
        Main.loadFxml(changeToRegisterButton.scene, "activity_sign_up.fxml", "Регистрация")
    }

    fun signIn() {
        GlobalScope.launch {
            // Build connection and rpc objects
            val managedChannel = ManagedChannelBuilder.forAddress(
                Main.SERVER_ADDRESS,
                Main.SERVER_PORT
            ).usePlaintext().build()
            val blockingStub = newBlockingStub(managedChannel)
            val phoneText = phoneNumberEdit.text //countryCodePicker.selectedCountryCode + phoneEditText!!.text
            val passwordText = passwordEdit.text //countryCodePicker.selectedCountryCode + phoneEditText!!.text
            val loginRequest = LoginRequest.newBuilder()
                .setApi(Main.API_VERSION)
                .setLogin(phoneText)
                .setPassword(passwordText)
                .setUserType(2)
                .build()
            val loginResponse: LoginResponse
            try {
                loginResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                    .loginUser(loginRequest) // Запрос на создание
                val authToken = loginResponse.authToken
                managedChannel.shutdown()
                if (authToken.isNotEmpty()) {
                    // Save data to preferences and start new activity
                    pref.putLong("last_login", Date().time) // Token saving into SharedPreferences
                    pref.putInt("dispatcher_id", loginResponse.userId)// Token saving into SharedPreferences
                    pref.put("auth_token", authToken)// Token saving into SharedPreferences
                    pref.put("login", phoneText)// Token saving into SharedPreferences
                    Platform.runLater {
                        Main.loadFxml(phoneNumberEdit.scene, "activity_main.fxml", "Диспетчер")
                    }
                } else {
                    phoneNumberEdit.text = ""
                    passwordEdit.text = ""
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

    fun validatePasswordEdit() {
        // TODO validation
/*
        if (passwordEdit.scene == null) {
            val scene = passwordEdit.scene
            passwordEdit = scene?.lookup("#passwordEdit") as JFXTextField
            passwordLabel = scene.lookup("#passwordLabel") as Label
        }
        validation.isPasswordValid(passwordEdit, passwordLabel)
*/
    }
}
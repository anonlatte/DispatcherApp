package com.example.dispatcher_app

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ActivitySignUp : Application() {
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

    override fun start(primaryStage: Stage?) {
    }

    // TODO validation

    fun signInScene() {
        Main.loadFxml(signInLabel.scene, "activity_sign_in.fxml", "Вход")
    }

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
                managedChannel.shutdown()
                // TODO вывод сообщения об ошибке
                /*
                if (e.status.cause is java.net.ConnectException) {
                    runOnUiThread { Toast.makeText(this@SignUpActivity, R.string.error_internet_connection, Toast.LENGTH_LONG).show() }
                } else if (e.status.code == Status.UNKNOWN.code) {
                    runOnUiThread { Toast.makeText(this@SignUpActivity, R.string.user_is_already_exists, Toast.LENGTH_LONG).show() }
                }
*//*
                //logger.log(Level.WARNING, "RPC failed: " + e.getStatus());
                e.printStackTrace()
                managedChannel.shutdown()
            }
        }*/
            }
        }
    }
}
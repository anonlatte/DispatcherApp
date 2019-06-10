package com.example.dispatcher_app

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
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences


class ActivityMakeOrder : Initializable {

    private lateinit var pref: Preferences
    @FXML
    lateinit var errorLabel: Label
    @FXML
    lateinit var successLabel: Label
    @FXML
    lateinit var startPointEdit: JFXTextField
    @FXML
    lateinit var phoneNumber: JFXTextField
    @FXML
    lateinit var destinationEdit: JFXTextField
    @FXML
    lateinit var additionalInfoEdit: JFXTextField
    @FXML
    lateinit var makeOrderButton: JFXButton

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pref = Preferences.userNodeForPackage(Main::class.java)
    }

    @FXML
    fun makeOrder() {
        // TODO validation
        if (startPointEdit.text.isNotEmpty() && destinationEdit.text.isNotEmpty() && phoneNumber.text.isNotEmpty()
        ) {
            GlobalScope.launch {
                val managedChannel = ManagedChannelBuilder.forAddress(
                    Main.SERVER_ADDRESS,
                    Main.SERVER_PORT
                ).usePlaintext().build()
                val blockingStub = taxiServiceGrpc.newBlockingStub(managedChannel)
                val customer = Customer.newBuilder().setPhoneNumber(phoneNumber.text).build()
                val cabRide = CabRide.newBuilder()
                    .setStartingPoint(startPointEdit.text)
                    .setEndingPoint(destinationEdit.text)
                    .build()
                val createCabRideDispatcherRequest = CreateCabRideDispatcherRequest.newBuilder()
                    .setApi(Main.API_VERSION)
                    .setCustomer(customer)
                    .setCabRide(cabRide)
                    .build()
                val createCabRideDispatcherResponse: CreateCabRideDispatcherResponse
                try {
                    createCabRideDispatcherResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                        .createCabRideDispatcher(createCabRideDispatcherRequest) // Запрос на создание
                    val setDetailsToOrderRequest = SetDetailsToOrderRequest.newBuilder()
                        .setApi(Main.API_VERSION)
                        .setCabRideId(createCabRideDispatcherResponse.cabRideId)
                        .setMessage(additionalInfoEdit.text)
                        .build()
                    if (additionalInfoEdit.text.isNotEmpty()) {
                        try {
                            val setDetailsToOrderResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                                .setDetailsToOrder(setDetailsToOrderRequest)
                            Platform.runLater {
                                successLabel.isVisible = true
                                (successLabel.scene.window as Stage).close()
                            }
                        } catch (e: StatusRuntimeException) {
                            Platform.runLater {
                                errorLabel.text = "Заказ был создан, но адрес не был отправлен"
                                errorLabel.isVisible = true
                            }
                            e.printStackTrace()
                        }
                    }
                    managedChannel.shutdown()
                } catch (e: StatusRuntimeException) {
                    errorLabel.text = "Заказ не был создан"
                    errorLabel.isVisible = true
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
}
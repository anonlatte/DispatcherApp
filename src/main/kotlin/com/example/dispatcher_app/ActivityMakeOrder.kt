package com.example.dispatcher_app

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import javafx.fxml.FXML
import javafx.fxml.Initializable
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
    lateinit var erroLabel: Label
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
        if (startPointEdit.text.isNotEmpty() && destinationEdit.text.isNotEmpty()
            && additionalInfoEdit.text.isNotEmpty() && phoneNumber.text.isNotEmpty()
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
                    try {
                        val setDetailsToOrderResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                            .setDetailsToOrder(setDetailsToOrderRequest)
                        successLabel.isVisible = true
                        (successLabel.scene.window as Stage).close()
                    } catch (e: StatusRuntimeException) {
                        erroLabel.text = "Заказ был создан, но адрес не был отправлен"
                        erroLabel.isVisible = true
                        e.printStackTrace()
                    }
                    managedChannel.shutdown()
                } catch (e: StatusRuntimeException) {
                    // TODO Check exceptions
                    erroLabel.text = "Заказ не был создан"
                    erroLabel.isVisible = true
                    e.printStackTrace()
/*
                if (e.status.cause is java.net.ConnectException) {
                    runOnUiThread { Toast.makeText(this@SignInActivity, R.string.error_internet_connection, Toast.LENGTH_LONG).show() }
                } else if (e.status.code == Status.Code.NOT_FOUND || e.status.code == Status.Code.PERMISSION_DENIED) {
                    runOnUiThread { Toast.makeText(this@SignInActivity, R.string.error_wrong_data, Toast.LENGTH_LONG).show() }
                } else if (e.status.code == Status.Code.UNKNOWN) {
                    runOnUiThread { Toast.makeText(this@SignInActivity, R.string.error_message_server, Toast.LENGTH_LONG).show() }
                }
*/
                    //                                logger.log(Level.WARNING, "RPC failed: " + e.getStatus());
                    managedChannel.shutdown()
                }
            }
        }
    }
}
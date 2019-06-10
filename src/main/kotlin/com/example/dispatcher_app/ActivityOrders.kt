package com.example.dispatcher_app

import com.example.dispatcher_app.taxiServiceGrpc.newBlockingStub
import com.jfoenix.controls.*
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.TextInputDialog
import javafx.scene.control.TreeItem
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences


class ActivityOrders : Initializable {

    private var ordersTableTree: TreeItem<CabRideInfo>? = null

    private lateinit var pref: Preferences
    @FXML
    lateinit var updateButton: JFXButton
    @FXML
    lateinit var ordersTable: JFXTreeTableView<CabRideInfo>
    @FXML
    lateinit var idColumn: JFXTreeTableColumn<CabRideInfo, Int>
    @FXML
    lateinit var customerIdColumn: JFXTreeTableColumn<CabRideInfo, Int>
    @FXML
    lateinit var rideStartTimeColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var rideEndTimeColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var startingPointColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var entranceColumn: JFXTreeTableColumn<CabRideInfo, Int>
    @FXML
    lateinit var endingPointColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var isCanceledColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var isOrderedForAnotherColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var isPendingOrderColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var paymentTypeColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var priceColumn: JFXTreeTableColumn<CabRideInfo, Int>
    @FXML
    lateinit var commentColumn: JFXTreeTableColumn<CabRideInfo, String>
    @FXML
    lateinit var feedbackColumn: JFXTreeTableColumn<CabRideInfo, String>


    @FXML
    lateinit var searchBar: JFXTextField

    private val customersTableData = FXCollections.observableArrayList<CabRideInfo>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pref = Preferences.userNodeForPackage(Main::class.java)
        initColumns()
        getOrders()

        // Set filter
        searchBar.textProperty().addListener { _, _, newValue ->
            if (newValue.isEmpty()) {
                ordersTable.setRoot(ordersTableTree)
            } else {
                val filteredRoot = TreeItem<CabRideInfo>()
                filter(ordersTableTree, newValue, filteredRoot)
                ordersTable.setRoot(filteredRoot)
            }
        }
    }

    private fun filter(root: TreeItem<CabRideInfo>?, filter: String, filteredRoot: TreeItem<CabRideInfo>?) {
        for (child in root!!.children) {
            val filteredChild = TreeItem<CabRideInfo>()
            filteredChild.value = child.value
            filteredChild.isExpanded = true
            filter(child, filter, filteredChild)
            if (filteredChild.children.isNotEmpty() || isMatch(filteredChild.value, filter)) {
                filteredRoot!!.children.add(filteredChild)
            }
        }
    }

    private fun isMatch(value: CabRideInfo?, filter: String): Boolean {
        if (filter.isEmpty()) {
            return true
        }

        val lowerCaseFilter = filter.toLowerCase()

        return when {
            value!!.comment.toLowerCase().contains(lowerCaseFilter) -> true
            value.startingPoint.toLowerCase().contains(lowerCaseFilter) -> true
            value.endingPoint.toLowerCase().contains(lowerCaseFilter) -> true
            // Does not match.
            else -> false
        }
    }

    private fun initColumns() {
        idColumn.cellValueFactory = Callback {
            return@Callback SimpleIntegerProperty(it.value.value.cabRideId).asObject()
        }
        customerIdColumn.cellValueFactory = Callback {
            return@Callback SimpleIntegerProperty(it.value.value.customerId).asObject()
        }
        rideStartTimeColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.rideStartTime)
        }
        rideEndTimeColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.rideEndTime)
        }
        startingPointColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.startingPoint)
        }
        entranceColumn.cellValueFactory = Callback {
            return@Callback SimpleIntegerProperty(it.value.value.entrance).asObject()
        }
        endingPointColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.endingPoint)
        }
        isCanceledColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.canceled)
        }
        isOrderedForAnotherColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.orderForAnother)
        }
        isPendingOrderColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.pendingOrder)
        }
        paymentTypeColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.paymentType)
        }
        priceColumn.cellValueFactory = Callback {
            return@Callback SimpleIntegerProperty(it.value.value.price).asObject()
        }
        commentColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.comment)
        }
        feedbackColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.feedback)
        }
    }

    @FXML
    private fun getOrders() {
        GlobalScope.launch {
            // Build connection and rpc objects
            val managedChannel = ManagedChannelBuilder.forAddress(
                Main.SERVER_ADDRESS,
                Main.SERVER_PORT
            ).usePlaintext().build()
            val blockingStub = newBlockingStub(managedChannel)
            val readAllCabRidesRequest = ReadAllCabRidesRequest.newBuilder()
                .setApi(Main.API_VERSION)
                .build()
            val readAllCabRidesResponse: ReadAllCabRidesResponse
            try {
                readAllCabRidesResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                    .readAllCabRides(readAllCabRidesRequest) // Запрос на создание
                managedChannel.shutdown()

                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                fun checkDate(timeStamp: Long): String {
                    return if (timeStamp > 1) {
                        simpleDateFormat.format(timeStamp * 1000)
                    } else {
                        ""
                    }
                }

                customersTableData.clear()
                readAllCabRidesResponse.cabRideList.forEach {
                    customersTableData.add(
                        CabRideInfo(
                            it.id,
                            it.customerId,
                            checkDate(it.rideStartTime.seconds),
                            checkDate(it.rideEndTime.seconds),
                            it.startingPoint,
                            it.entrance,
                            it.endingPoint,
                            it.canceled.toString(),
                            it.orderForAnother.toString(),
                            it.pendingOrder.toString(),
                            it.paymentTypeId.toString(),
                            it.price,
                            it.comment,
                            it.feedback
                        )
                    )
                }

                Platform.runLater {
                    ordersTable.isShowRoot = false
                    ordersTableTree =
                        RecursiveTreeItem<CabRideInfo>(
                            customersTableData,
                            RecursiveTreeObject<CabRideInfo>::getChildren
                        )
                    ordersTable.columns.setAll(
                        idColumn,
                        customerIdColumn,
                        rideStartTimeColumn,
                        rideEndTimeColumn,
                        startingPointColumn,
                        entranceColumn,
                        endingPointColumn,
                        isCanceledColumn,
                        isOrderedForAnotherColumn,
                        isPendingOrderColumn,
                        paymentTypeColumn,
                        priceColumn,
                        commentColumn,
                        feedbackColumn
                    )
                    ordersTable.root = ordersTableTree
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

    @FXML
    fun cancelOrder() {
        val selectedOrder = ordersTable.selectionModel.selectedItem.value

        if (selectedOrder != null && selectedOrder.rideEndTime.isEmpty()) {
            GlobalScope.launch {

                val managedChannel = ManagedChannelBuilder.forAddress(
                    Main.SERVER_ADDRESS,
                    Main.SERVER_PORT
                ).usePlaintext().build()
                val blockingStub = newBlockingStub(managedChannel)
                val deleteCabRideRequest = DeleteCabRideRequest.newBuilder()
                    .setApi(Main.API_VERSION)
                    .setCabRideId(selectedOrder.cabRideId)
                    .build()
                try {
                    val deleteCabRideResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                        .deleteCabRide(deleteCabRideRequest) // Запрос на создание
                    managedChannel.shutdown()
                    getOrders()
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

    @FXML
    fun addInfo() {
        val selectedOrder = ordersTable.selectionModel.selectedItem.value

        if (selectedOrder != null) {
            val dialog = TextInputDialog()
            dialog.title = "Добавить информацию о заказе"
            val result = dialog.showAndWait()

            result.ifPresent { info ->
                GlobalScope.launch {

                    val managedChannel = ManagedChannelBuilder.forAddress(
                        Main.SERVER_ADDRESS,
                        Main.SERVER_PORT
                    ).usePlaintext().build()
                    val blockingStub = newBlockingStub(managedChannel)
                    val setDetailsToOrderRequest = SetDetailsToOrderRequest.newBuilder()
                        .setApi(Main.API_VERSION)
                        .setCabRideId(selectedOrder.cabRideId)
                        .setMessage(info)
                        .build()
                    try {
                        val setDetailsToOrderResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                            .setDetailsToOrder(setDetailsToOrderRequest) // Запрос на создание
                        managedChannel.shutdown()
                        getOrders()
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
    }

    @FXML
    fun saveDataToExcel() {
        Main.saveTableToFile(ordersTable, "orders")
    }
}
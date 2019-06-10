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
import javafx.scene.control.TreeItem
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences


class ActivityCustomers : Initializable {

    private var customersTableTree: TreeItem<CustomerInfo>? = null

    private lateinit var pref: Preferences
    @FXML
    lateinit var updateButton: JFXButton
    @FXML
    lateinit var customersTable: JFXTreeTableView<CustomerInfo>
    @FXML
    lateinit var idColumn: JFXTreeTableColumn<CustomerInfo, Int>
    @FXML
    lateinit var nameColumn: JFXTreeTableColumn<CustomerInfo, String>
    @FXML
    lateinit var phoneColumn: JFXTreeTableColumn<CustomerInfo, String>
    @FXML
    lateinit var emailColumn: JFXTreeTableColumn<CustomerInfo, String>
    @FXML
    lateinit var createDateColumn: JFXTreeTableColumn<CustomerInfo, String>
    @FXML
    lateinit var searchBar: JFXTextField

    private val customersTableData = FXCollections.observableArrayList<CustomerInfo>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pref = Preferences.userNodeForPackage(Main::class.java)
        initColumns()
        getCustomers()

        // Set filter
        searchBar.textProperty().addListener { _, _, newValue ->
            if (newValue.isEmpty()) {
                customersTable.setRoot(customersTableTree)
            } else {
                val filteredRoot = TreeItem<CustomerInfo>()
                filter(customersTableTree, newValue, filteredRoot)
                customersTable.setRoot(filteredRoot)
            }
        }
    }

    private fun filter(root: TreeItem<CustomerInfo>?, filter: String, filteredRoot: TreeItem<CustomerInfo>?) {
        for (child in root!!.children) {
            val filteredChild = TreeItem<CustomerInfo>()
            filteredChild.value = child.value
            filteredChild.isExpanded = true
            filter(child, filter, filteredChild)
            if (filteredChild.children.isNotEmpty() || isMatch(filteredChild.value, filter)) {
                filteredRoot!!.children.add(filteredChild)
            }
        }
    }

    private fun isMatch(value: CustomerInfo?, filter: String): Boolean {
        if (filter.isEmpty()) {
            return true
        }

        val lowerCaseFilter = filter.toLowerCase()

        return when {
            value!!.name.toLowerCase().contains(lowerCaseFilter) -> true
            value.phoneNumber.toLowerCase().contains(lowerCaseFilter) -> true
            value.email.toLowerCase().contains(lowerCaseFilter) -> true
            // Does not match.
            else -> false
        }
    }

    private fun initColumns() {
        idColumn.cellValueFactory = Callback {
            return@Callback SimpleIntegerProperty(it.value.value.customerId).asObject()
        }
        nameColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.name)
        }

        phoneColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.phoneNumber)
        }

        emailColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.email)
        }

        createDateColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.createTime)
        }
    }

    @FXML
    private fun getCustomers() {
        // Build connection and rpc objects
        GlobalScope.launch {
            val managedChannel = ManagedChannelBuilder.forAddress(
                Main.SERVER_ADDRESS,
                Main.SERVER_PORT
            ).usePlaintext().build()
            val blockingStub = newBlockingStub(managedChannel)
            val readAllCustomersRequest = ReadAllCustomersRequest.newBuilder()
                .setApi(Main.API_VERSION)
                .build()
            val readAllCustomersResponse: ReadAllCustomersResponse
            try {
                readAllCustomersResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                    .readAllCustomers(readAllCustomersRequest) // Запрос на создание
                managedChannel.shutdown()

                val creationDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                customersTableData.clear()
                readAllCustomersResponse.customerList.forEach {
                    customersTableData.add(
                        CustomerInfo(
                            it.id,
                            it.name,
                            it.phoneNumber,
                            it.email,
                            creationDateFormat.format(it.createTime.seconds * 1000)
                        )
                    )
                }
                Platform.runLater {
                    customersTable.isShowRoot = false
                    customersTableTree =
                        RecursiveTreeItem<CustomerInfo>(
                            customersTableData,
                            RecursiveTreeObject<CustomerInfo>::getChildren
                        )
                    customersTable.columns.setAll(
                        idColumn,
                        nameColumn,
                        phoneColumn,
                        emailColumn,
                        createDateColumn
                    )
                    customersTable.root = customersTableTree
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

    fun showStatistic() {
        val selectedCustomer = customersTable.selectionModel.selectedItem.value

        if (selectedCustomer != null) {
            GlobalScope.launch {
                val managedChannel = ManagedChannelBuilder.forAddress(
                    Main.SERVER_ADDRESS,
                    Main.SERVER_PORT
                ).usePlaintext().build()
                val blockingStub = newBlockingStub(managedChannel)
                val readCustomerStatisticRequest = ReadCustomerStatisticRequest.newBuilder()
                    .setApi(Main.API_VERSION)
                    .setCustomerId(selectedCustomer.customerId)
                    .build()
                val readCustomerStatisticResponse: ReadCustomerStatisticResponse
                try {
                    readCustomerStatisticResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                        .readCustomerStatistic(readCustomerStatisticRequest) // Запрос на создание
                    managedChannel.shutdown()

                    Platform.runLater {
                        val alert = Alert(Alert.AlertType.INFORMATION)
                        alert.title =
                            "Статистика " + selectedCustomer.name + " " + selectedCustomer.phoneNumber + " " + selectedCustomer.email
                        alert.headerText =
                            "Количество заказов:" + readCustomerStatisticResponse.orderedCabRides.toString()
                        alert.showAndWait()
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

    @FXML
    fun saveDataToExcel() {
        Main.saveTableToFile(customersTable, "customers")
    }
}
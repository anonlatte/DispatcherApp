package com.example.dispatcher_app

import com.example.dispatcher_app.taxiServiceGrpc.newBlockingStub
import com.jfoenix.controls.*
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.MenuItem
import javafx.scene.control.TreeItem
import javafx.util.Callback
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences


class ActivityMain : Initializable {

    private var driversTableTree: TreeItem<DriverInfo>? = null

    private lateinit var pref: Preferences
    @FXML
    lateinit var makeOrderMenu: MenuItem
    @FXML
    lateinit var clientsList: MenuItem
    @FXML
    lateinit var verificationButton: JFXButton
    @FXML
    lateinit var updateButton: JFXButton
    @FXML
    lateinit var driversTable: JFXTreeTableView<DriverInfo>
    @FXML
    lateinit var idColumn: JFXTreeTableColumn<DriverInfo, Int>
    @FXML
    lateinit var surnameColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var nameColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var patronymicColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var birthDateColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var phoneColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var statusColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var emailColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var activatedColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var createDateColumn: JFXTreeTableColumn<DriverInfo, String>
    @FXML
    lateinit var searchBar: JFXTextField
    @FXML
    lateinit var unverifiedToggle: JFXToggleButton
    @FXML
    lateinit var onWorkToggle: JFXToggleButton

    private val driversTableData = FXCollections.observableArrayList<DriverInfo>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pref = Preferences.userNodeForPackage(Main::class.java)
        initColumns()
        getDrivers()

        // Set filter
        searchBar.textProperty().addListener { _, _, newValue ->
            if (newValue.isEmpty()) {
                driversTable.setRoot(driversTableTree)
            } else {
                val filteredRoot = TreeItem<DriverInfo>()
                filter(driversTableTree, newValue, filteredRoot)
                driversTable.setRoot(filteredRoot)
            }
        }

        unverifiedToggle.selectedProperty().addListener { _, _, isVerified ->
            if (unverifiedToggle.isSelected) {
                val filteredRoot = TreeItem<DriverInfo>()
                filterVerifiedDrivers(driversTableTree, isVerified, filteredRoot)
                driversTable.setRoot(filteredRoot)
            } else {
                driversTable.setRoot(driversTableTree)
            }
        }
        onWorkToggle.selectedProperty().addListener { _, _, isWorking ->
            if (onWorkToggle.isSelected) {
                val filteredRoot = TreeItem<DriverInfo>()
                filterWorkingDrivers(driversTableTree, isWorking, filteredRoot)
                driversTable.setRoot(filteredRoot)
            } else {
                driversTable.setRoot(driversTableTree)
            }
        }

        makeOrderMenu.setOnAction {
            showOrderDialog()
        }

        clientsList.setOnAction {
            Main.loadFxml("activity_customers.fxml", "Список клиентов")
        }
    }

    private fun filterWorkingDrivers(
        driversTableTree: TreeItem<DriverInfo>?, working: Boolean?, filteredRoot: TreeItem<DriverInfo>
    ) {
        for (child in driversTableTree!!.children) {
            val filteredChild = TreeItem<DriverInfo>()
            filteredChild.value = child.value
            filteredChild.isExpanded = true
            filterWorkingDrivers(child, working, filteredChild)
            val workingString = if (working!!) {
                "Работает"
            } else {
                "Не работает"
            }
            if (filteredChild.children.isNotEmpty() || filteredChild.value.isWorking == workingString) {
                filteredRoot.children.add(filteredChild)
            }
        }
    }


    private fun filterVerifiedDrivers(
        driversTableTree: TreeItem<DriverInfo>?, verified: Boolean?, filteredRoot: TreeItem<DriverInfo>
    ) {
        for (child in driversTableTree!!.children) {
            val filteredChild = TreeItem<DriverInfo>()
            filteredChild.value = child.value
            filteredChild.isExpanded = true
            filterVerifiedDrivers(child, verified, filteredChild)
            val verifiedString = if (verified!!) {
                "Активирован"
            } else {
                "Не активирован"
            }
            if (filteredChild.children.isNotEmpty() || filteredChild.value.isActivated != verifiedString) {
                filteredRoot.children.add(filteredChild)
            }
        }
    }

    private fun filter(root: TreeItem<DriverInfo>?, filter: String, filteredRoot: TreeItem<DriverInfo>?) {
        for (child in root!!.children) {
            val filteredChild = TreeItem<DriverInfo>()
            filteredChild.value = child.value
            filteredChild.isExpanded = true
            filter(child, filter, filteredChild)
            if (filteredChild.children.isNotEmpty() || isMatch(filteredChild.value, filter)) {
                filteredRoot!!.children.add(filteredChild)
            }
        }
    }

    private fun isMatch(value: DriverInfo?, filter: String): Boolean {
        if (filter.isEmpty()) {
            return true
        }

        val lowerCaseFilter = filter.toLowerCase()

        return when {
            value!!.firstName.toLowerCase().contains(lowerCaseFilter) -> true
            value.surname.toLowerCase().contains(lowerCaseFilter) -> true
            value.patronymic.toLowerCase().contains(lowerCaseFilter) -> true
            value.driverId.toString().contains(lowerCaseFilter) -> true
            value.birthDate.toLowerCase().contains(lowerCaseFilter) -> true
            value.phoneNumber.toLowerCase().contains(lowerCaseFilter) -> true
            value.email.toLowerCase().contains(lowerCaseFilter) -> true
            // Does not match.
            else -> false
        }
    }

    private fun initColumns() {
        idColumn.cellValueFactory = Callback {
            return@Callback SimpleIntegerProperty(it.value.value.driverId).asObject()
        }
        surnameColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.surname)
        }
        nameColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.firstName)
        }
        patronymicColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.patronymic)
        }
        birthDateColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.birthDate)
        }
        phoneColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.phoneNumber)
        }
        statusColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.isWorking)
        }
        emailColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.email)
        }
        activatedColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.isActivated)
        }
        createDateColumn.cellValueFactory = Callback {
            return@Callback SimpleStringProperty(it.value.value.createTime)
        }
    }

    @FXML
    private fun getDrivers() {
        // Build connection and rpc objects
        val managedChannel = ManagedChannelBuilder.forAddress(
            Main.SERVER_ADDRESS,
            Main.SERVER_PORT
        ).usePlaintext().build()
        val blockingStub = newBlockingStub(managedChannel)
        val readAllDriversRequest = ReadAllDriversRequest.newBuilder()
            .setApi(Main.API_VERSION)
            .build()
        val readAllDriversResponse: ReadAllDriversResponse
        try {
            readAllDriversResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                .readAllDrivers(readAllDriversRequest) // Запрос на создание
            managedChannel.shutdown()

            val birthDateFormat = SimpleDateFormat("yyy-MM-dd")
            val creationDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            var workingStatus: String
            var verifiedStatus: String
            driversTableData.clear()
            readAllDriversResponse.driverList.forEach {
                workingStatus = if (it.working) {
                    "Работает"
                } else {
                    "Не работает"
                }
                verifiedStatus = if (it.activated) {
                    "Активирован"
                } else {
                    "Не активирован"
                }
                driversTableData.add(
                    DriverInfo(
                        it.id, it.firstName, it.surname, it.partronymic,
                        birthDateFormat.format(it.birthDate.seconds * 1000), it.phoneNumber, workingStatus,
                        it.email, verifiedStatus, creationDateFormat.format(it.createTime.seconds * 1000)
                    )
                )
            }
            Platform.runLater {
                driversTable.isShowRoot = false
                driversTableTree =
                    RecursiveTreeItem<DriverInfo>(driversTableData, RecursiveTreeObject<DriverInfo>::getChildren)
                driversTable.columns.setAll(
                    idColumn,
                    surnameColumn,
                    nameColumn,
                    patronymicColumn,
                    birthDateColumn,
                    phoneColumn,
                    statusColumn,
                    emailColumn,
                    activatedColumn,
                    createDateColumn
                )
                driversTable.root = driversTableTree
            }

        } catch (e: StatusRuntimeException) {
            // TODO Check exceptions
            // TODO вывод сообщения об ошибке
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

    @FXML
    fun verifyDriver() {
        val selectedDriver = driversTable.selectionModel.selectedItem.value


        if (selectedDriver != null) {
            val managedChannel = ManagedChannelBuilder.forAddress(
                Main.SERVER_ADDRESS,
                Main.SERVER_PORT
            ).usePlaintext().build()
            val blockingStub = newBlockingStub(managedChannel)
            val verifyDriversAccountRequest = VerifyDriversAccountRequest.newBuilder()
                .setApi(Main.API_VERSION)
                .setDriverId(selectedDriver.driverId)
                .build()
            val verifyDriversAccountResponse: VerifyDriversAccountResponse
            try {
                verifyDriversAccountResponse = blockingStub.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)
                    .verifyDriversAccount(verifyDriversAccountRequest) // Запрос на создание
                managedChannel.shutdown()
                getDrivers()
            } catch (e: StatusRuntimeException) {
                // TODO Check exceptions
                // TODO вывод сообщения об ошибке
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

    @FXML
    fun showOrderDialog() {
        Main.loadFxml("activity_make_order.fxml", "Создание заказа")
    }
}
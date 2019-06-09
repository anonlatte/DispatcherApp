package com.example.dispatcher_app

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.MenuItem
import java.net.URL
import java.util.*
import java.util.prefs.Preferences


class ActivityMakeOrder : Initializable {

    private lateinit var pref: Preferences
    @FXML
    lateinit var makeOrderMenu: MenuItem


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pref = Preferences.userNodeForPackage(Main::class.java)

    }
}
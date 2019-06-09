package com.example.dispatcher_app

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject

class DriverInfo constructor(
    var driverId: Int,
    var firstName: String,
    var surname: String,
    var patronymic: String,
    var birthDate: String,
    var phoneNumber: String,
    var isWorking: String,
    var email: String,
    var isActivated: String,
    var createTime: String
) : RecursiveTreeObject<DriverInfo>()
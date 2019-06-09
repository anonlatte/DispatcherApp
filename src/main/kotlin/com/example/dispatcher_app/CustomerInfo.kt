package com.example.dispatcher_app

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject

class CustomerInfo constructor(
    var customerId: Int,
    var name: String,
    var phoneNumber: String,
    var email: String,
    var createTime: String
) : RecursiveTreeObject<CustomerInfo>()
package com.example.dispatcher_app

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject

class CabRideInfo constructor(
    var cabRideId: Int,
    var customerId: Int,
    var rideStartTime: String,
    var rideEndTime: String,
    var startingPoint: String,
    var entrance: Int,
    var endingPoint: String,
    var canceled: String,
    var orderForAnother: String,
    var pendingOrder: String,
    var paymentType: String,
    var price: Int,
    var comment: String,
    var feedback: String
) : RecursiveTreeObject<CabRideInfo>()
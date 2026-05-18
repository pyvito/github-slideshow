package com.wled.manager.model

data class WledDevice(
    val ip: String,
    var name: String = ip,
    var isOnline: Boolean = false
)
